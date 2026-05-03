package com.pidev.models;

public class ProblemSolution {
        private int id;
        private Activity activity;
        private String problemDescription;
        private String groupSolution;
        private String supervisorSolution;

        public ProblemSolution() {
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public Activity getActivity() {
            return activity;
        }

        public void setActivity(Activity activity) {
            this.activity = activity;
        }

        public String getProblemDescription() {
            return problemDescription;
        }

        public void setProblemDescription(String problemDescription) {
            this.problemDescription = problemDescription;
        }

        public String getGroupSolution() {
            return groupSolution;
        }

        public void setGroupSolution(String groupSolution) {
            this.groupSolution = groupSolution;
        }

        public String getSupervisorSolution() {
            return supervisorSolution;
        }

        public void setSupervisorSolution(String supervisorSolution) {
            this.supervisorSolution = supervisorSolution;
        }
    @Override
    public String toString() {
        return problemDescription != null ? problemDescription : "No description available";
    }


}
