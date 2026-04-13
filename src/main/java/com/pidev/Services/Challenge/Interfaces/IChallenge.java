package com.pidev.Services.Challenge.Interfaces;


import com.pidev.models.Challenge;

import java.util.List;

public interface IChallenge {
    List<Challenge> findChallengeWithActivities(int u);
    List<Challenge> displayALL();
    List<Challenge> searchChallenge(String query);
    List<Challenge> displayForSupervisor(int u);


}
