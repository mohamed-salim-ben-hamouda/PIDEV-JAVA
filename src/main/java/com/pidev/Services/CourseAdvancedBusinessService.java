package com.pidev.Services;

import com.pidev.models.Course;
import com.pidev.models.Quiz;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Metiers avances de gestion de cours.
 *
 * Ce service centralise:
 * - la normalisation d'un cours avant persistence,
 * - les regles metier de validation,
 * - l'evaluation de la completude d'un cours,
 * - la recommandation des prochains cours a suivre.
 */
public class CourseAdvancedBusinessService {

    public static final int MAX_RECOMMENDATIONS_DEFAULT = 3;

    public Course normalizeForPersistence(Course source) {
        if (source == null) {
            return null;
        }

        source.setTitle(trimToNull(source.getTitle()));
        source.setDescription(trimToNull(source.getDescription()));
        source.setContent(trimToNull(source.getContent()));
        source.setMaterial(trimToNull(source.getMaterial()));

        String normalizedDifficulty = normalizeDifficulty(source.getDifficulty());
        source.setDifficulty(normalizedDifficulty);

        if (source.getDuration() < 0) {
            source.setDuration(0);
        }

        if (source.getValidationScore() < 0) {
            source.setValidationScore(0);
        } else if (source.getValidationScore() > 100) {
            source.setValidationScore(100);
        }

        if (source.getSectionsToReview() == null) {
            source.setSectionsToReview(new ArrayList<>());
        } else {
            List<String> cleaned = source.getSectionsToReview().stream()
                    .map(this::trimToNull)
                    .filter(value -> value != null)
                    .distinct()
                    .toList();
            source.setSectionsToReview(new ArrayList<>(cleaned));
        }

        return source;
    }

    public List<String> validateForPersistence(Course course) {
        List<String> errors = new ArrayList<>();
        if (course == null) {
            errors.add("Le cours est obligatoire.");
            return errors;
        }

        if (isBlank(course.getTitle())) {
            errors.add("Le titre est obligatoire.");
        } else if (course.getTitle().length() < 3 || course.getTitle().length() > 60) {
            errors.add("Le titre doit contenir entre 3 et 60 caracteres.");
        }

        if (isBlank(course.getDescription())) {
            errors.add("La description est obligatoire.");
        }

        if (course.getDuration() <= 0) {
            errors.add("La duree doit etre superieure a 0.");
        }

        if (course.getValidationScore() < 0 || course.getValidationScore() > 100) {
            errors.add("Le score de validation doit etre entre 0 et 100.");
        }

        if (course.getCreator() == null || course.getCreator().getId() == null) {
            errors.add("Le superviseur (creator) est obligatoire.");
        }

        if (isBlank(course.getContent())) {
            errors.add("Le contenu du cours (PDF/reference) est obligatoire.");
        }

        if (course.getMaterial() != null && course.getMaterial().length() > 255) {
            errors.add("Le support (material) ne doit pas depasser 255 caracteres.");
        }

        if (!Course.DIFFICULTY_LEVELS.containsKey(normalizeDifficulty(course.getDifficulty()))) {
            errors.add("La difficulte doit etre BEGINNER, INTERMEDIATE ou ADVANCED.");
        }

        if (course.getSectionsToReview() != null && course.getSectionsToReview().size() > 20) {
            errors.add("Le nombre de sections a revoir est limite a 20.");
        }

        return errors;
    }

    public CourseCompleteness evaluateCompleteness(Course course) {
        if (course == null) {
            return new CourseCompleteness(0, List.of("Cours absent"), "incomplete");
        }

        int score = 0;
        List<String> missing = new ArrayList<>();

        if (!isBlank(course.getTitle())) score += 15; else missing.add("Titre");
        if (!isBlank(course.getDescription())) score += 15; else missing.add("Description");
        if (course.getDuration() > 0) score += 15; else missing.add("Duree");
        if (!isBlank(course.getDifficulty())) score += 10; else missing.add("Difficulte");
        if (course.getValidationScore() >= 0 && course.getValidationScore() <= 100) score += 10; else missing.add("Score validation");
        if (!isBlank(course.getContent())) score += 20; else missing.add("Contenu/PDF");
        if (course.getCreator() != null && course.getCreator().getId() != null) score += 10; else missing.add("Superviseur");
        if (course.getSectionsToReview() != null && !course.getSectionsToReview().isEmpty()) score += 5; else missing.add("Sections a revoir");

        String status = score >= 80 ? "ready" : score >= 50 ? "partial" : "incomplete";
        return new CourseCompleteness(score, missing, status);
    }

    public List<CourseSuggestion> suggestNextCourses(List<Course> catalog, Course referenceCourse, int maxResults) {
        if (catalog == null || catalog.isEmpty()) {
            return List.of();
        }

        int safeMax = maxResults <= 0 ? MAX_RECOMMENDATIONS_DEFAULT : maxResults;
        int referenceLevel = referenceCourse != null ? referenceCourse.getDifficultyLevel() : 1;
        Integer referenceId = referenceCourse != null ? referenceCourse.getId() : null;

        Set<Integer> excludedIds = new HashSet<>();
        if (referenceId != null) {
            excludedIds.add(referenceId);
        }

        List<CourseSuggestion> ranked = new ArrayList<>();
        for (Course candidate : catalog) {
            if (candidate == null || candidate.getId() == null) {
                continue;
            }
            if (excludedIds.contains(candidate.getId())) {
                continue;
            }
            if (!candidate.isIsActive()) {
                continue;
            }

            ScoreResult result = scoreCandidate(candidate, referenceLevel);
            ranked.add(new CourseSuggestion(candidate, result.score(), result.reason(), badgeFromScore(result.score())));
        }

        return ranked.stream()
                .sorted(Comparator
                        .comparingInt(CourseSuggestion::priorityScore)
                        .reversed()
                        .thenComparing(cs -> cs.course().getTitle(), String.CASE_INSENSITIVE_ORDER))
                .limit(safeMax)
                .toList();
    }

    private ScoreResult scoreCandidate(Course candidate, int referenceLevel) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        int candidateLevel = candidate.getDifficultyLevel();
        if (candidateLevel == referenceLevel + 1) {
            score += 40;
            reasons.add("niveau suivant logique");
        } else if (candidateLevel == referenceLevel) {
            score += 25;
            reasons.add("consolidation du niveau");
        } else if (candidateLevel < referenceLevel) {
            score += 10;
            reasons.add("revision");
        } else {
            score += 5;
            reasons.add("niveau avance");
        }

        if (candidate.getPrerequisiteQuiz() == null || candidate.getPrerequisiteQuiz().getId() == null) {
            score += 20;
            reasons.add("sans prerequis bloquant");
        } else {
            score += 10;
            reasons.add("avec prerequis");
        }

        if (candidate.getValidationScore() >= 80) {
            score += 20;
            reasons.add("qualite pedagogique elevee");
        } else if (candidate.getValidationScore() >= 60) {
            score += 10;
            reasons.add("qualite pedagogique solide");
        }

        if (!isBlank(candidate.getMaterial())) {
            score += 5;
            reasons.add("support additionnel disponible");
        }

        if (candidate.getSectionsToReview() != null && !candidate.getSectionsToReview().isEmpty()) {
            score += 5;
            reasons.add("plan de revision present");
        }

        return new ScoreResult(Math.min(score, 100), String.join(" + ", reasons));
    }

    private String badgeFromScore(int score) {
        if (score >= 80) {
            return "Prochaine etape logique";
        }
        if (score >= 50) {
            return "Bon choix";
        }
        return "Recommande";
    }

    private String normalizeDifficulty(String value) {
        if (isBlank(value)) {
            return Course.DIFFICULTY_BEGINNER;
        }

        String upper = value.trim().toUpperCase(Locale.ROOT);
        if (Course.DIFFICULTY_LEVELS.containsKey(upper)) {
            return upper;
        }
        return Course.DIFFICULTY_BEGINNER;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record CourseCompleteness(int completenessScore, List<String> missingFields, String status) {}

    public record CourseSuggestion(Course course, int priorityScore, String reason, String badge) {}

    private record ScoreResult(int score, String reason) {}
}
