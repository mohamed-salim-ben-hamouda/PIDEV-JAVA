package com.pidev.Services;

import com.pidev.models.Question;
import java.util.List;

public class AiFeedbackService {

    /**
     * Analyse les questions echouees et retourne un feedback constructif.
     */
    public String generateFeedback(List<Question> failedQuestions) {
        if (failedQuestions == null || failedQuestions.isEmpty()) {
            return "Bravo, aucune lacune detectee !";
        }

        StringBuilder feedback = new StringBuilder("🤖 Feedback de l'IA :\n");
        feedback.append("J'ai remarque quelques difficultes sur les points suivants :\n");

        int count = 0;
        for (Question q : failedQuestions) {
            if (count >= 3) {
                feedback.append("- ... et d'autres points.\n");
                break;
            }
            String content = q.getContent();
            if (content != null && content.length() > 30) {
                content = content.substring(0, 30) + "...";
            }
            feedback.append("- Concept autour de : \"").append(content).append("\"\n");
            count++;
        }

        feedback.append("\n💡 Conseil : Prenez le temps de relire le chapitre correspondant avant de retenter votre chance.");
        return feedback.toString();
    }
}
