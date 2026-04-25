package com.pidev.Services;

import java.util.HashMap;
import java.util.Map;

public class TranslationService {

    private static final Map<String, String> translationCache = new HashMap<>();
    private static final Map<String, String> dictionary = buildDictionary();
    
    /**
     * Service de traduction local (mock) - À remplacer par une vraie API avec clé API
     * Pour utiliser une vraie API: Google Translate, DeepL, etc.
     */
    public String translate(String text, String targetLang) {
        if (text == null || text.isBlank()) {
            return text;
        }
        
        // Vérifier le cache
        String cacheKey = text + "|" + targetLang;
        if (translationCache.containsKey(cacheKey)) {
            System.out.println("[TranslationService] ✓ Traduction depuis cache");
            return translationCache.get(cacheKey);
        }
        
        try {
            // Simuler un délai réseau (50-100ms)
            Thread.sleep(50);
            
            String result = performLocalTranslation(text, targetLang);
            translationCache.put(cacheKey, result);
            
            System.out.println("[TranslationService] ✓ Traduction réussie");
            return result;
            
        } catch (Exception e) {
            System.err.println("[TranslationService] Exception: " + e.getMessage());
            translationCache.put(cacheKey, text);
            return text;
        }
    }
    
    /**
     * Effectue une traduction locale basique en utilisant un dictionnaire
     * REMARQUE: Ceci est une implémentation temporaire/mock.
     * À remplacer par une vraie API de traduction (Google Translate, DeepL, etc.)
     * avec votre clé API personnelle.
     */
    private String performLocalTranslation(String text, String targetLang) {
        if ("fr".equalsIgnoreCase(targetLang)) {
            return translateToFrench(text);
        } else {
            return translateToEnglish(text);
        }
    }
    
    /**
     * Traduction basique vers le français
     */
    private String translateToFrench(String text) {
        String result = text;
        
        // Remplacer les mots clés courants (par ordre de longueur décroissante pour éviter les conflits)
        for (Map.Entry<String, String> entry : dictionary.entrySet()) {
            String english = entry.getKey();
            String french = entry.getValue();
            result = replaceWordIgnoreCase(result, english, french);
        }
        
        return result;
    }
    
    /**
     * Traduction basique vers l'anglais
     */
    private String translateToEnglish(String text) {
        String result = text;
        
        // Inverser le dictionnaire (par ordre de longueur décroissante)
        for (Map.Entry<String, String> entry : dictionary.entrySet()) {
            String english = entry.getKey();
            String french = entry.getValue();
            
            // Gérer les variantes
            String[] frenchVariants = french.split("/");
            for (String variant : frenchVariants) {
                result = replaceWordIgnoreCase(result, variant.trim(), english);
            }
        }
        
        return result;
    }
    
    /**
     * Remplace un mot en ignorant la casse et les frontières de mots
     */
    private String replaceWordIgnoreCase(String text, String search, String replacement) {
        if (search == null || search.isEmpty() || text == null) {
            return text;
        }
        
        StringBuilder result = new StringBuilder();
        String lowerText = text.toLowerCase();
        String lowerSearch = search.toLowerCase();
        int lastIndex = 0;
        int index = lowerText.indexOf(lowerSearch);
        
        while (index != -1) {
            // Vérifier les frontières de mots
            boolean validStart = (index == 0 || !Character.isLetterOrDigit(lowerText.charAt(index - 1)));
            boolean validEnd = (index + lowerSearch.length() >= lowerText.length() || 
                              !Character.isLetterOrDigit(lowerText.charAt(index + lowerSearch.length())));
            
            if (validStart && validEnd) {
                // Préserver la casse du premier caractère si possible
                String actualReplacement = replacement;
                if (index < text.length() && Character.isUpperCase(text.charAt(index))) {
                    if (actualReplacement.length() > 0) {
                        actualReplacement = Character.toUpperCase(actualReplacement.charAt(0)) + 
                                          actualReplacement.substring(1).toLowerCase();
                    }
                }
                
                result.append(text, lastIndex, index);
                result.append(actualReplacement);
                lastIndex = index + lowerSearch.length();
            } else {
                result.append(text.charAt(index));
                lastIndex = index + 1;
            }
            
            index = lowerText.indexOf(lowerSearch, lastIndex);
        }
        
        result.append(text.substring(lastIndex));
        return result.toString();
    }
    
    /**
     * Dictionnaire de traduction complet
     */
    private static Map<String, String> buildDictionary() {
        Map<String, String> dict = new HashMap<>();
        
        // Articles et petits mots
        dict.put("the", "le/la/les");
        dict.put("a", "un/une");
        dict.put("an", "un/une");
        dict.put("and", "et");
        dict.put("or", "ou");
        dict.put("but", "mais");
        dict.put("not", "ne pas");
        dict.put("is", "est");
        dict.put("are", "sont");
        dict.put("be", "être");
        dict.put("was", "était");
        dict.put("were", "étaient");
        dict.put("to", "à/pour");
        dict.put("in", "dans/en");
        dict.put("on", "sur");
        dict.put("at", "à");
        dict.put("by", "par");
        dict.put("from", "de");
        dict.put("with", "avec");
        dict.put("for", "pour");
        dict.put("of", "de");
        
        // Verbes courants
        dict.put("have", "avoir");
        dict.put("has", "a");
        dict.put("do", "faire");
        dict.put("does", "fait");
        dict.put("can", "peut");
        dict.put("could", "pourrait");
        dict.put("would", "serait");
        dict.put("should", "devrait");
        dict.put("will", "sera");
        dict.put("shall", "va");
        dict.put("may", "peut");
        dict.put("might", "pourrait");
        dict.put("must", "doit");
        dict.put("need", "besoin");
        dict.put("want", "vouloir");
        dict.put("like", "aimer");
        dict.put("love", "adorer");
        dict.put("hate", "détester");
        dict.put("know", "savoir");
        dict.put("think", "penser");
        dict.put("believe", "croire");
        dict.put("see", "voir");
        dict.put("hear", "entendre");
        dict.put("speak", "parler");
        dict.put("talk", "parler");
        dict.put("say", "dire");
        dict.put("tell", "dire");
        dict.put("ask", "demander");
        dict.put("answer", "répondre/réponse");
        dict.put("write", "écrire");
        dict.put("read", "lire");
        dict.put("understand", "comprendre");
        dict.put("learn", "apprendre");
        dict.put("teach", "enseigner");
        dict.put("help", "aider");
        dict.put("work", "travailler");
        dict.put("make", "faire");
        dict.put("come", "venir");
        dict.put("go", "aller");
        dict.put("give", "donner");
        dict.put("take", "prendre");
        dict.put("get", "obtenir");
        dict.put("find", "trouver");
        dict.put("use", "utiliser");
        dict.put("try", "essayer");
        dict.put("start", "commencer");
        dict.put("stop", "arrêter");
        dict.put("continue", "continuer");
        dict.put("change", "changer");
        dict.put("decide", "décider");
        dict.put("succeed", "réussir");
        dict.put("fail", "échouer");
        dict.put("win", "gagner");
        dict.put("lose", "perdre");
        dict.put("play", "jouer");
        dict.put("watch", "regarder");
        dict.put("listen", "écouter");
        
        // Noms courants
        dict.put("person", "personne");
        dict.put("people", "personnes");
        dict.put("man", "homme");
        dict.put("woman", "femme");
        dict.put("child", "enfant");
        dict.put("time", "temps");
        dict.put("day", "jour");
        dict.put("week", "semaine");
        dict.put("month", "mois");
        dict.put("year", "année");
        dict.put("hour", "heure");
        dict.put("minute", "minute");
        dict.put("second", "seconde");
        dict.put("place", "endroit");
        dict.put("house", "maison");
        dict.put("home", "maison");
        dict.put("school", "école");
        dict.put("work", "travail");
        dict.put("office", "bureau");
        dict.put("thing", "chose");
        dict.put("word", "mot");
        dict.put("name", "nom");
        dict.put("number", "nombre");
        dict.put("question", "question");
        dict.put("answer", "réponse");
        dict.put("idea", "idée");
        dict.put("problem", "problème");
        dict.put("solution", "solution");
        dict.put("reason", "raison");
        dict.put("way", "façon");
        dict.put("example", "exemple");
        dict.put("money", "argent");
        dict.put("price", "prix");
        dict.put("cost", "coût");
        dict.put("value", "valeur");
        dict.put("result", "résultat");
        dict.put("success", "succès");
        dict.put("failure", "échec");
        dict.put("life", "vie");
        dict.put("death", "mort");
        dict.put("love", "amour");
        dict.put("hate", "haine");
        dict.put("fear", "peur");
        dict.put("hope", "espoir");
        dict.put("truth", "vérité");
        dict.put("lie", "mensonge");
        
        // Adjectifs courants
        dict.put("good", "bien/bon");
        dict.put("bad", "mauvais");
        dict.put("big", "grand");
        dict.put("small", "petit");
        dict.put("new", "nouveau");
        dict.put("old", "vieux");
        dict.put("young", "jeune");
        dict.put("long", "long");
        dict.put("short", "court");
        dict.put("high", "haut");
        dict.put("low", "bas");
        dict.put("fast", "rapide");
        dict.put("slow", "lent");
        dict.put("easy", "facile");
        dict.put("difficult", "difficile");
        dict.put("hard", "difficile");
        dict.put("simple", "simple");
        dict.put("complex", "complexe");
        dict.put("important", "important");
        dict.put("special", "spécial");
        dict.put("different", "différent");
        dict.put("same", "même");
        dict.put("similar", "similaire");
        dict.put("right", "droit/correct");
        dict.put("wrong", "faux");
        dict.put("true", "vrai");
        dict.put("false", "faux");
        dict.put("possible", "possible");
        dict.put("impossible", "impossible");
        dict.put("certain", "certain");
        dict.put("uncertain", "incertain");
        dict.put("sure", "sûr");
        dict.put("better", "meilleur");
        dict.put("worse", "pire");
        dict.put("best", "meilleur");
        dict.put("worst", "pire");
        dict.put("happy", "heureux");
        dict.put("sad", "triste");
        dict.put("angry", "en colère");
        dict.put("calm", "calme");
        dict.put("quiet", "silencieux");
        dict.put("loud", "bruyant");
        dict.put("warm", "chaud");
        dict.put("cold", "froid");
        dict.put("hot", "chaud");
        dict.put("cool", "frais");
        dict.put("wet", "mouillé");
        dict.put("dry", "sec");
        dict.put("clean", "propre");
        dict.put("dirty", "sale");
        dict.put("bright", "brillant");
        dict.put("dark", "sombre");
        dict.put("light", "léger");
        dict.put("heavy", "lourd");
        
        // Termes éducatifs
        dict.put("quiz", "quiz");
        dict.put("course", "cours");
        dict.put("chapter", "chapitre");
        dict.put("lesson", "leçon");
        dict.put("study", "étude");
        dict.put("exam", "examen");
        dict.put("test", "test");
        dict.put("grade", "note");
        dict.put("score", "score");
        dict.put("student", "étudiant");
        dict.put("teacher", "professeur");
        dict.put("school", "école");
        dict.put("class", "classe");
        dict.put("subject", "sujet");
        dict.put("topic", "sujet");
        dict.put("skill", "compétence");
        dict.put("knowledge", "connaissance");
        dict.put("education", "éducation");
        dict.put("learning", "apprentissage");
        dict.put("teaching", "enseignement");
        dict.put("homework", "devoir");
        dict.put("assignment", "devoir");
        dict.put("project", "projet");
        dict.put("research", "recherche");
        dict.put("experiment", "expérience");
        dict.put("theory", "théorie");
        dict.put("practice", "pratique");
        dict.put("exercise", "exercice");
        dict.put("activity", "activité");
        dict.put("participation", "participation");
        dict.put("discussion", "discussion");
        dict.put("debate", "débat");
        dict.put("presentation", "présentation");
        dict.put("report", "rapport");
        dict.put("essay", "essai");
        dict.put("book", "livre");
        dict.put("textbook", "manuel");
        dict.put("notebook", "cahier");
        dict.put("paper", "papier");
        dict.put("pen", "stylo");
        dict.put("pencil", "crayon");
        dict.put("computer", "ordinateur");
        dict.put("internet", "internet");
        dict.put("website", "site web");
        dict.put("email", "email");
        dict.put("online", "en ligne");
        dict.put("offline", "hors ligne");
        dict.put("digital", "numérique");
        dict.put("technology", "technologie");
        dict.put("application", "application");
        dict.put("software", "logiciel");
        dict.put("program", "programme");
        dict.put("data", "données");
        dict.put("information", "information");
        dict.put("system", "système");
        dict.put("process", "processus");
        dict.put("method", "méthode");
        dict.put("strategy", "stratégie");
        dict.put("approach", "approche");
        dict.put("technique", "technique");
        dict.put("procedure", "procédure");
        
        // Termes de communication
        dict.put("style", "style");
        dict.put("collaboration", "collaboration");
        dict.put("avoidance", "évitement");
        dict.put("competing", "compétition");
        dict.put("compromising", "compromis");
        dict.put("accommodating", "adaptation");
        dict.put("communication", "communication");
        dict.put("conflict", "conflit");
        dict.put("resolution", "résolution");
        dict.put("negotiation", "négociation");
        dict.put("discussion", "discussion");
        dict.put("conversation", "conversation");
        dict.put("dialogue", "dialogue");
        dict.put("feedback", "retour");
        dict.put("advice", "conseil");
        dict.put("suggestion", "suggestion");
        dict.put("opinion", "opinion");
        dict.put("agreement", "accord");
        dict.put("disagreement", "désaccord");
        dict.put("decision", "décision");
        dict.put("choice", "choix");
        dict.put("option", "option");
        dict.put("alternative", "alternative");
        dict.put("preference", "préférence");
        dict.put("priority", "priorité");
        dict.put("goal", "objectif");
        dict.put("objective", "objectif");
        dict.put("target", "cible");
        dict.put("effort", "effort");
        dict.put("attempt", "tentative");
        dict.put("try", "essai");
        dict.put("success", "succès");
        dict.put("failure", "échec");
        dict.put("achievement", "réussite");
        dict.put("accomplishment", "accomplissement");
        dict.put("progress", "progrès");
        dict.put("improvement", "amélioration");
        dict.put("development", "développement");
        dict.put("growth", "croissance");
        dict.put("challenge", "défi");
        dict.put("difficulty", "difficulté");
        dict.put("obstacle", "obstacle");
        dict.put("opportunity", "opportunité");
        dict.put("potential", "potentiel");
        dict.put("ability", "capacité");
        dict.put("capability", "capacité");
        dict.put("strength", "force");
        dict.put("weakness", "faiblesse");
        dict.put("advantage", "avantage");
        dict.put("disadvantage", "inconvénient");
        dict.put("benefit", "bénéfice");
        dict.put("drawback", "inconvénient");
        dict.put("risk", "risque");
        dict.put("threat", "menace");
        dict.put("opportunity", "opportunité");
        dict.put("requirement", "exigence");
        dict.put("standard", "standard");
        dict.put("criterion", "critère");
        dict.put("measure", "mesure");
        dict.put("assessment", "évaluation");
        dict.put("evaluation", "évaluation");
        dict.put("feedback", "retour");
        dict.put("review", "examen");
        dict.put("analysis", "analyse");
        dict.put("synthesis", "synthèse");
        dict.put("conclusion", "conclusion");
        dict.put("recommendation", "recommandation");
        dict.put("suggestion", "suggestion");
        
        // Phrases complètes communes
        dict.put("what is", "quel est");
        dict.put("how is", "comment est");
        dict.put("who is", "qui est");
        dict.put("where is", "où est");
        dict.put("when is", "quand est");
        dict.put("why is", "pourquoi est");
        dict.put("how are you", "comment allez-vous");
        dict.put("nice to meet you", "ravi de vous rencontrer");
        dict.put("please", "s'il vous plaît");
        dict.put("thank you", "merci");
        dict.put("thanks", "merci");
        dict.put("you're welcome", "de rien");
        dict.put("good morning", "bonjour");
        dict.put("good afternoon", "bonjour");
        dict.put("good evening", "bonsoir");
        dict.put("good night", "bonne nuit");
        dict.put("hello", "bonjour");
        dict.put("goodbye", "au revoir");
        dict.put("bye", "au revoir");
        dict.put("see you", "à bientôt");
        dict.put("see you later", "à plus tard");
        dict.put("have a good day", "bonne journée");
        
        return dict;
    }
}

