package com.pidev.Services.Challenge.Interfaces;

import com.pidev.models.Activity;
import com.pidev.models.ProblemSolution;

import java.util.List;

public interface IProblemSolution {
    int addProblem(ProblemSolution p, int a);
    void addSolutionGrp(ProblemSolution p);
    List<ProblemSolution> displayProblems(int a);
    List<ProblemSolution> display(int a);
    void update(ProblemSolution p);
    void delete(int id);
    void updateSupervisorSolution(ProblemSolution p);
    boolean isSupervisorSolution(ProblemSolution p);
}
