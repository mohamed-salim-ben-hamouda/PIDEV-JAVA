package com.pidev.Services.Challenge.Interfaces;

import com.pidev.models.Activity;
import com.pidev.models.ProblemSolution;

import java.util.List;

public interface IProblemSolution {
    int addProblem(ProblemSolution p, int a);
    void addSolutionGrp(ProblemSolution p);
    List<ProblemSolution> displayProblems(int a);
}
