package com.pidev.Services;

import com.pidev.models.Cv;
import com.pidev.models.User;
import com.pidev.models.Certif;
import com.pidev.models.Education;
import com.pidev.models.Experience;
import com.pidev.models.Langue;
import com.pidev.models.Skill;
import com.pidev.utils.DataSource;

import java.net.URI;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CVService {
    private static final Set<String> LANGUES_AUTORISEES = Set.of("Francais", "Anglais", "Arabe");
    private final Connection connection;

    public CVService() {
        this.connection = DataSource.getInstance().getConnection();
        if (this.connection == null) {
            throw new IllegalStateException("Connexion MySQL indisponible");
        }
        ensureTableSchema();
    }

    private void ensureTableSchema() {
        try {
            // Check and add missing columns for experience
            addColumnIfNotExists("experience", "location", "VARCHAR(255)");
            addColumnIfNotExists("experience", "start_date", "DATE");
            addColumnIfNotExists("experience", "end_date", "DATE");
            addColumnIfNotExists("experience", "currently_working", "TINYINT(1) DEFAULT 0");
            addColumnIfNotExists("experience", "description", "TEXT");

            // Check and add missing columns for education
            addColumnIfNotExists("education", "field_of_study", "VARCHAR(255)");
            addColumnIfNotExists("education", "city", "VARCHAR(255)");
            addColumnIfNotExists("education", "start_date", "DATE");
            addColumnIfNotExists("education", "end_date", "DATE");
            addColumnIfNotExists("education", "description", "TEXT");

            // Check and add missing columns for skill
            addColumnIfNotExists("skill", "type", "VARCHAR(50)");

            // Check and add missing columns for certif
            addColumnIfNotExists("certif", "issue_date", "DATE");
            addColumnIfNotExists("certif", "exp_date", "DATE");

        } catch (SQLException e) {
            System.err.println("Note: Erreur lors de la vérification du schéma (possible colonnes déjà existantes): " + e.getMessage());
        }
    }

    private void addColumnIfNotExists(String tableName, String columnName, String definition) throws SQLException {
        try {
            // Try to select the column to see if it exists
            String checkSql = "SELECT " + columnName + " FROM " + tableName + " LIMIT 1";
            try (Statement stmt = connection.createStatement()) {
                stmt.executeQuery(checkSql);
            }
        } catch (SQLException e) {
            // Column doesn't exist, so add it
            String alterSql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition;
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(alterSql);
                System.out.println("Colonne ajoutée: " + tableName + "." + columnName);
            }
        }
    }

    public Cv ajouter(Cv cv) throws SQLException {
        validateCv(cv, false);

        String sql = "INSERT INTO cv (nom_cv, langue, id_template, progression, creation_date, updated_at, user_id, linkedin_url, summary) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillCvStatement(preparedStatement, cv);
            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Insertion du CV échouée");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    cv.setId(generatedKeys.getInt(1));
                }
            }
        }

        // Save related entities
        saveRelatedEntities(cv);

        return cv;
    }

    private void saveRelatedEntities(Cv cv) throws SQLException {
        // Delete existing first if it's an update (simple approach)
        deleteRelatedEntities(cv.getId());

        // Save Experiences
        if (cv.getExperiences() != null) {
            for (Experience exp : cv.getExperiences()) {
                String sql = "INSERT INTO experience (cv_id, job_title, company, location, start_date, end_date, currently_working, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, cv.getId());
                    ps.setString(2, exp.getJobTitle());
                    ps.setString(3, exp.getCompany());
                    ps.setString(4, exp.getLocation());
                    ps.setDate(5, exp.getStartDate() != null ? Date.valueOf(exp.getStartDate()) : null);
                    ps.setDate(6, exp.getEndDate() != null ? Date.valueOf(exp.getEndDate()) : null);
                    ps.setBoolean(7, exp.getCurrentlyWorking() != null ? exp.getCurrentlyWorking() : false);
                    ps.setString(8, exp.getDescription());
                    ps.executeUpdate();
                }
            }
        }
        // Save Educations
        if (cv.getEducations() != null) {
            for (Education edu : cv.getEducations()) {
                String sql = "INSERT INTO education (cv_id, degree, field_of_study, school, city, start_date, end_date, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, cv.getId());
                    ps.setString(2, edu.getDegree());
                    ps.setString(3, edu.getFieldOfStudy());
                    ps.setString(4, edu.getSchool());
                    ps.setString(5, edu.getCity());
                    ps.setDate(6, edu.getStartDate() != null ? Date.valueOf(edu.getStartDate()) : null);
                    ps.setDate(7, edu.getEndDate() != null ? Date.valueOf(edu.getEndDate()) : null);
                    ps.setString(8, edu.getDescription());
                    ps.executeUpdate();
                }
            }
        }
        // Save Skills
        if (cv.getSkills() != null) {
            for (Skill skill : cv.getSkills()) {
                String sql = "INSERT INTO skill (cv_id, nom, type, level) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, cv.getId());
                    ps.setString(2, skill.getNom());
                    ps.setString(3, skill.getType());
                    ps.setString(4, skill.getLevel());
                    ps.executeUpdate();
                }
            }
        }
        // Save Certifs
        if (cv.getCertifs() != null) {
            for (Certif cert : cv.getCertifs()) {
                String sql = "INSERT INTO certif (cv_id, name, issued_by, issue_date, exp_date) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, cv.getId());
                    ps.setString(2, cert.getName());
                    ps.setString(3, cert.getIssuedBy());
                    ps.setDate(4, cert.getIssueDate() != null ? Date.valueOf(cert.getIssueDate()) : null);
                    ps.setDate(5, cert.getExpDate() != null ? Date.valueOf(cert.getExpDate()) : null);
                    ps.executeUpdate();
                }
            }
        }
        // Save Languages
        if (cv.getLanguages() != null) {
            for (Langue lang : cv.getLanguages()) {
                String sql = "INSERT INTO langue (cv_id, nom, niveau) VALUES (?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, cv.getId());
                    ps.setString(2, lang.getNom());
                    ps.setString(3, lang.getNiveau());
                    ps.executeUpdate();
                }
            }
        }
    }

    private void deleteRelatedEntities(Integer cvId) throws SQLException {
        if (cvId == null) return;
        String[] tables = {"experience", "education", "skill", "certif", "langue"};
        for (String table : tables) {
            String sql = "DELETE FROM " + table + " WHERE cv_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, cvId);
                ps.executeUpdate();
            }
        }
    }

    public List<Cv> afficher() throws SQLException {
        String sql = "SELECT id, nom_cv, langue, id_template, progression, creation_date, updated_at, user_id, linkedin_url, summary "
                + "FROM cv ORDER BY id DESC";
        List<Cv> cvs = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                Cv cv = mapCv(resultSet);
                fetchRelatedEntities(cv);
                cvs.add(cv);
            }
        }

        return cvs;
    }

    private void fetchRelatedEntities(Cv cv) throws SQLException {
        // Fetch Experiences
        String expSql = "SELECT job_title, company, location, start_date, end_date, currently_working, description FROM experience WHERE cv_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(expSql)) {
            ps.setInt(1, cv.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Experience exp = new Experience();
                    exp.setJobTitle(rs.getString("job_title"));
                    exp.setCompany(rs.getString("company"));
                    exp.setLocation(rs.getString("location"));
                    exp.setStartDate(rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null);
                    exp.setEndDate(rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null);
                    exp.setCurrentlyWorking(rs.getBoolean("currently_working"));
                    exp.setDescription(rs.getString("description"));
                    cv.getExperiences().add(exp);
                }
            }
        }
        // Fetch Educations
        String eduSql = "SELECT degree, field_of_study, school, city, start_date, end_date, description FROM education WHERE cv_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(eduSql)) {
            ps.setInt(1, cv.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Education edu = new Education();
                    edu.setDegree(rs.getString("degree"));
                    edu.setFieldOfStudy(rs.getString("field_of_study"));
                    edu.setSchool(rs.getString("school"));
                    edu.setCity(rs.getString("city"));
                    edu.setStartDate(rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null);
                    edu.setEndDate(rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null);
                    edu.setDescription(rs.getString("description"));
                    cv.getEducations().add(edu);
                }
            }
        }
        // Fetch Skills
        String skillSql = "SELECT nom, type, level FROM skill WHERE cv_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(skillSql)) {
            ps.setInt(1, cv.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Skill skill = new Skill();
                    skill.setNom(rs.getString("nom"));
                    skill.setType(rs.getString("type"));
                    skill.setLevel(rs.getString("level"));
                    cv.getSkills().add(skill);
                }
            }
        }
        // Fetch Certifs
        String certSql = "SELECT name, issued_by, issue_date, exp_date FROM certif WHERE cv_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(certSql)) {
            ps.setInt(1, cv.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Certif cert = new Certif();
                    cert.setName(rs.getString("name"));
                    cert.setIssuedBy(rs.getString("issued_by"));
                    cert.setIssueDate(rs.getDate("issue_date") != null ? rs.getDate("issue_date").toLocalDate() : null);
                    cert.setExpDate(rs.getDate("exp_date") != null ? rs.getDate("exp_date").toLocalDate() : null);
                    cv.getCertifs().add(cert);
                }
            }
        }
        // Fetch Languages
        String langSql = "SELECT nom, niveau FROM langue WHERE cv_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(langSql)) {
            ps.setInt(1, cv.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Langue lang = new Langue();
                    lang.setNom(rs.getString("nom"));
                    lang.setNiveau(rs.getString("niveau"));
                    cv.getLanguages().add(lang);
                }
            }
        }
    }

    /**
     * Updates the CV and all its related entities (Experience, Education, etc.)
     * in a single transaction.
     * @param cv The CV object with updated data and its related entity lists
     * @return true if update was successful
     * @throws SQLException if a database error occurs
     */
    public boolean updateFullCv(Cv cv) throws SQLException {
        validateCv(cv, true);

        try {
            // Start transaction
            connection.setAutoCommit(false);

            // 1. Update CV basic info
            String updateCvSql = "UPDATE cv SET nom_cv = ?, langue = ?, id_template = ?, progression = ?, creation_date = ?, "
                    + "updated_at = ?, user_id = ?, linkedin_url = ?, summary = ? WHERE id = ?";

            try (PreparedStatement ps = connection.prepareStatement(updateCvSql)) {
                fillCvStatement(ps, cv);
                ps.setInt(10, cv.getId());
                ps.executeUpdate();
            }

            // 2. Delete all related entities first (Experience, Education, etc.)
            deleteRelatedEntities(cv.getId());

            // 3. Insert new related entities
            // Insert Experiences
            if (cv.getExperiences() != null && !cv.getExperiences().isEmpty()) {
                String expSql = "INSERT INTO experience (cv_id, job_title, company, location, start_date, end_date, currently_working, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(expSql)) {
                    for (Experience exp : cv.getExperiences()) {
                        ps.setInt(1, cv.getId());
                        ps.setString(2, exp.getJobTitle());
                        ps.setString(3, exp.getCompany());
                        ps.setString(4, exp.getLocation());
                        ps.setDate(5, exp.getStartDate() != null ? Date.valueOf(exp.getStartDate()) : null);
                        ps.setDate(6, exp.getEndDate() != null ? Date.valueOf(exp.getEndDate()) : null);
                        ps.setBoolean(7, exp.getCurrentlyWorking() != null ? exp.getCurrentlyWorking() : false);
                        ps.setString(8, exp.getDescription());
                        ps.executeUpdate();
                    }
                }
            }

            // Insert Educations
            if (cv.getEducations() != null && !cv.getEducations().isEmpty()) {
                String eduSql = "INSERT INTO education (cv_id, degree, field_of_study, school, city, start_date, end_date, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(eduSql)) {
                    for (Education edu : cv.getEducations()) {
                        ps.setInt(1, cv.getId());
                        ps.setString(2, edu.getDegree());
                        ps.setString(3, edu.getFieldOfStudy());
                        ps.setString(4, edu.getSchool());
                        ps.setString(5, edu.getCity());
                        ps.setDate(6, edu.getStartDate() != null ? Date.valueOf(edu.getStartDate()) : null);
                        ps.setDate(7, edu.getEndDate() != null ? Date.valueOf(edu.getEndDate()) : null);
                        ps.setString(8, edu.getDescription());
                        ps.executeUpdate();
                    }
                }
            }

            // Insert Skills
            if (cv.getSkills() != null && !cv.getSkills().isEmpty()) {
                String skillSql = "INSERT INTO skill (cv_id, nom, type, level) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(skillSql)) {
                    for (Skill skill : cv.getSkills()) {
                        ps.setInt(1, cv.getId());
                        ps.setString(2, skill.getNom());
                        ps.setString(3, skill.getType());
                        ps.setString(4, skill.getLevel());
                        ps.executeUpdate();
                    }
                }
            }

            // Insert Certifs
            if (cv.getCertifs() != null && !cv.getCertifs().isEmpty()) {
                String certSql = "INSERT INTO certif (cv_id, name, issued_by, issue_date, exp_date) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(certSql)) {
                    for (Certif cert : cv.getCertifs()) {
                        ps.setInt(1, cv.getId());
                        ps.setString(2, cert.getName());
                        ps.setString(3, cert.getIssuedBy());
                        ps.setDate(4, cert.getIssueDate() != null ? Date.valueOf(cert.getIssueDate()) : null);
                        ps.setDate(5, cert.getExpDate() != null ? Date.valueOf(cert.getExpDate()) : null);
                        ps.executeUpdate();
                    }
                }
            }

            // Insert Languages
            if (cv.getLanguages() != null && !cv.getLanguages().isEmpty()) {
                String langSql = "INSERT INTO langue (cv_id, nom, niveau) VALUES (?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(langSql)) {
                    for (Langue lang : cv.getLanguages()) {
                        ps.setInt(1, cv.getId());
                        ps.setString(2, lang.getNom());
                        ps.setString(3, lang.getNiveau());
                        ps.executeUpdate();
                    }
                }
            }

            // Commit transaction
            connection.commit();
            return true;

        } catch (SQLException e) {
            // Rollback on error
            if (connection != null) {
                connection.rollback();
            }
            System.err.println("Erreur lors de la modification complète du CV: " + e.getMessage());
            throw e;
        } finally {
            // Restore default auto-commit behavior
            if (connection != null) {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean modifier(Cv cv) throws SQLException {
        return updateFullCv(cv);
    }

    public boolean supprimer(int id) throws SQLException {
        validateId(id, "ID du CV invalide");

        // Delete related entities first to avoid foreign key constraints issues
        deleteRelatedEntities(id);

        String sql = "DELETE FROM cv WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    private void fillCvStatement(PreparedStatement preparedStatement, Cv cv) throws SQLException {
        preparedStatement.setString(1, normalize(cv.getNomCv()));
        preparedStatement.setString(2, normalize(cv.getLangue()));
        setNullableInteger(preparedStatement, 3, cv.getIdTemplate());
        setNullableInteger(preparedStatement, 4, cv.getProgression());

        // Ensure creation date is never null to avoid Timestamp.valueOf(null) NPE
        LocalDateTime creationDate = cv.getCreationDate() != null ? cv.getCreationDate() : LocalDateTime.now();
        preparedStatement.setTimestamp(5, Timestamp.valueOf(creationDate));

        setNullableTimestamp(preparedStatement, 6, cv.getUpdatedAt());
        preparedStatement.setInt(7, cv.getUser().getId());
        setNullableString(preparedStatement, 8, cv.getLinkedinUrl());
        setNullableString(preparedStatement, 9, cv.getSummary());
    }

    private Cv mapCv(ResultSet resultSet) throws SQLException {
        Cv cv = new Cv();
        cv.setId(resultSet.getInt("id"));
        cv.setNomCv(resultSet.getString("nom_cv"));
        cv.setLangue(resultSet.getString("langue"));
        cv.setIdTemplate(getNullableInteger(resultSet, "id_template"));
        cv.setProgression(getNullableInteger(resultSet, "progression"));
        cv.setCreationDate(toLocalDateTime(resultSet.getTimestamp("creation_date")));
        cv.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));

        Integer userId = getNullableInteger(resultSet, "user_id");
        if (userId != null) {
            cv.setUser(new User(userId));
        }

        cv.setLinkedinUrl(resultSet.getString("linkedin_url"));
        cv.setSummary(resultSet.getString("summary"));
        return cv;
    }

    private void validateCv(Cv cv, boolean requireId) {
        if (cv == null) {
            throw new IllegalArgumentException("Le CV est obligatoire");
        }

        if (requireId) {
            validateId(cv.getId(), "L'ID du CV est obligatoire");
        }

        validateRequiredString(cv.getNomCv(), "Le nom du CV est obligatoire", 2, 30, "Le nom du CV doit contenir entre 2 et 30 caractères");
        validateRequiredString(cv.getLangue(), "La langue est obligatoire", 2, 30, "La langue doit contenir entre 2 et 30 caractères");

        if (!LANGUES_AUTORISEES.contains(normalize(cv.getLangue()))) {
            throw new IllegalArgumentException("La langue doit être Francais, Anglais ou Arabe");
        }

        if (cv.getIdTemplate() != null && cv.getIdTemplate() <= 0) {
            throw new IllegalArgumentException("L'ID template doit être positif");
        }

        if (cv.getProgression() != null && (cv.getProgression() < 0 || cv.getProgression() > 100)) {
            throw new IllegalArgumentException("La progression doit être comprise entre 0 et 100");
        }

        if (cv.getCreationDate() == null) {
            cv.setCreationDate(LocalDateTime.now());
        }

        if (cv.getUser() == null || cv.getUser().getId() == null || cv.getUser().getId() <= 0) {
            throw new IllegalArgumentException("L'utilisateur du CV est obligatoire");
        }

        if (cv.getLinkedinUrl() != null && !cv.getLinkedinUrl().isBlank()) {
            validateUrl(cv.getLinkedinUrl());
        }

        if (cv.getSummary() != null) {
            if (cv.getSummary().isBlank()) {
                throw new IllegalArgumentException("Le résumé ne peut pas être vide");
            }
            if (cv.getSummary().length() > 1000) {
                throw new IllegalArgumentException("Le résumé ne peut pas dépasser 1000 caractères");
            }
        }
    }

    private void validateRequiredString(String value, String emptyMessage, int minLength, int maxLength, String rangeMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(emptyMessage);
        }

        String normalized = normalize(value);
        if (normalized.length() < minLength || normalized.length() > maxLength) {
            throw new IllegalArgumentException(rangeMessage);
        }
    }

    private void validateUrl(String url) {
        try {
            URI uri = URI.create(url.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new IllegalArgumentException("L'URL LinkedIn est invalide");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("L'URL LinkedIn est invalide");
        }
    }

    private void validateId(Integer id, String message) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void setNullableInteger(PreparedStatement preparedStatement, int index, Integer value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, Types.INTEGER);
        } else {
            preparedStatement.setInt(index, value);
        }
    }

    private void setNullableTimestamp(PreparedStatement preparedStatement, int index, java.time.LocalDateTime value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, Types.TIMESTAMP);
        } else {
            preparedStatement.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    private void setNullableString(PreparedStatement preparedStatement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            preparedStatement.setNull(index, Types.VARCHAR);
        } else {
            preparedStatement.setString(index, value.trim());
        }
    }

    private Integer getNullableInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
