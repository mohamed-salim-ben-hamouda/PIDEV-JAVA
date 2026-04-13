package com.pidev.Services.Challenge.Interfaces;

import com.pidev.models.Activity;
import com.pidev.models.Evaluation;

public interface IEvaluation {
    void StartEvaluation(Evaluation e, Activity a);
    boolean isEvaluation(int a);
    void updateEvaluation(Evaluation e);
    Evaluation findEvaluation(int a);
    double SelectGrpScore(int e);
    void delete(long e);
}
