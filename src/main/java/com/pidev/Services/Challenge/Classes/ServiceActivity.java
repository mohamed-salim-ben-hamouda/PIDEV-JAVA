package com.pidev.Services.Challenge.Classes;

import com.pidev.Services.Challenge.Interfaces.IActivity;
import com.pidev.models.*;
import com.pidev.utils.DataSource;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class ServiceActivity implements IActivity {
    Connection connection;

    public ServiceActivity() {
        this.connection = DataSource.getInstance().getConnection();
    }

    @Override
    public void StartActivity(Activity a, Challenge c, Group g) {
        String query = "INSERT INTO ACTIVITY (id_challenge_id,group_id_id,status,start_time,repo_created) " +
                "VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getId());
            ps.setInt(2, g.getId());
            ps.setString(3, "in_progress");
            ps.setTimestamp(4, java.sql.Timestamp.valueOf(a.getActivity_start_time()));
            ps.setInt(5, 0);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    a.setId(keys.getInt(1));
                } else {
                    throw new SQLException("Activity created but no generated ID was returned.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Activity findActivityInprogress(int id) {
        String query = "SELECT a.id,a.status, a.id_challenge_id, a.group_id_id,a.repo_created, c.* FROM activity a " +
                "JOIN challenge c ON c.id = a.id_challenge_id " +
                "JOIN membership m ON m.group_id_id = a.group_id_id " +
                "WHERE m.user_id_id = ? " +
                "AND a.status = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.setString(2, "in_progress");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Activity activity = new Activity();
                activity.setStatus(rs.getString("status"));
                activity.setId(rs.getInt("id"));
                Group Group = new Group();
                Group.setId(rs.getInt("group_id_id"));
                activity.setGroup(Group);
                Challenge challenge = new Challenge();
                challenge.setId(rs.getInt("id_challenge_id"));
                challenge.setTitle(rs.getString("title"));
                challenge.setDescription(rs.getString("description"));
                challenge.setDifficulty(rs.getString("difficulty"));
                challenge.setContent(rs.getString("content"));
                challenge.setGithub(rs.getInt("github"));

                activity.setChallenge(challenge);

                return activity;

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isUserLeader(int group_id, int user_id) {
        String query = "SELECT role FROM membership WHERE group_id_id=? AND user_id_id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, group_id);
            ps.setInt(2, user_id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                return "leader".equalsIgnoreCase(role);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return false;
    }

    @Override
    public void submissionfile(Activity a) {
        String query = "UPDATE activity SET submission_file=? ,submission_date=?, status=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, a.getSubmissionFile());
            Timestamp now = new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(2, now);
            ps.setString(3, "submitted");
            ps.setInt(4, a.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isActivityPassedByGrp(int group_id, int challenge_id) {
        String query = "SELECT status FROM activity WHERE group_id_id=? AND id_challenge_id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, group_id);
            ps.setInt(2, challenge_id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String status = rs.getString("status");
                return "in_progress".equalsIgnoreCase(status);
            } else {
                return true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Group> findGroupsInActivity(int id_challenge) {
        List<Group> gr = new ArrayList<>();
        String query = "SELECT DISTINCT g.id, g.name FROM `group` g " +
                "JOIN activity a ON a.group_id_id = g.id " +
                "WHERE a.id_challenge_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id_challenge);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Group g = new Group();
                g.setId(rs.getInt("id"));
                g.setName(rs.getString("name"));
                gr.add(g);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return gr;
    }

    public String ActivityStatus(int challenge_id, int group_id) {
        String status = null;
        String query = "Select status FROM activity WHERE id_challenge_id = ? AND group_id_id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, challenge_id);
            ps.setInt(2, group_id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                status = rs.getString("status");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return status;
    }

    public Activity findActivityByChallengeAndGroup(int challengeId, int groupId) {
        String query = "SELECT a.id, a.status, a.submission_file, a.submission_date, a.repo_created, g.name AS group_name " +
                "FROM activity a " +
                "JOIN `group` g ON g.id = a.group_id_id " +
                "WHERE a.id_challenge_id = ? AND a.group_id_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, challengeId);
            ps.setInt(2, groupId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Activity activity = new Activity();
                activity.setId(rs.getInt("id"));
                activity.setStatus(rs.getString("status"));
                activity.setSubmissionFile(rs.getString("submission_file"));
                activity.setRepo_created(rs.getInt("repo_created"));

                Timestamp ts = rs.getTimestamp("submission_date");
                if (ts != null) {
                    LocalDateTime ldt = ts.toLocalDateTime();
                    activity.setSubmissionDate(ldt);
                }

                Group group = new Group();
                group.setId(groupId);
                group.setName(rs.getString("group_name"));
                activity.setGroup(group);

                Challenge challenge = new Challenge();
                challenge.setId(challengeId);
                activity.setChallenge(challenge);

                return activity;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Activity findActivityByChallengeAndGrp(Challenge c, Group g) {
        String query = "SELECT * FROM activity WHERE id_challenge_id=? AND group_id_id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, c.getId());
            ps.setInt(2, g.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Activity a = new Activity();
                a.setId(rs.getInt("id"));
                a.setSubmissionFile(rs.getString("submission_file"));
                a.setStatus(rs.getString("status"));
                a.setSubmissionDate(rs.getTimestamp("submission_date") != null ? rs.getTimestamp("submission_date").toLocalDateTime() : null);
                return a;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public void updateActivityStatus(Activity a) {
        String query = "UPDATE activity SET status=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, "evaluated");
            ps.setInt(2, a.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    @Override
    public List<Activity> getOldActivitiesForUser(int userId) {
        List<Activity> list = new ArrayList<>();
        String query = "SELECT DISTINCT " +
                "a.id AS activityId, " +
                "a.status AS activity_status, " +
                "a.group_id_id, " +
                "a.submission_file AS activity_submission, " +
                "g.name AS group_name, " +
                "c.id AS challenge_id, " +
                "c.title, " +
                "c.description, " +
                "c.difficulty, " +
                "c.dead_line, " +
                "c.github " +
                "FROM activity a " +
                "JOIN challenge c ON a.id_challenge_id = c.id " +
                "JOIN `group` g ON a.group_id_id = g.id " +
                "JOIN membership m ON m.group_id_id = a.group_id_id AND m.user_id_id = ? " +
                "WHERE a.status IN ('submitted', 'evaluated') " +
                "ORDER BY a.submission_date DESC";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Challenge challenge = new Challenge();
                challenge.setId(rs.getInt("challenge_id"));
                challenge.setTitle(rs.getString("title"));
                challenge.setDescription(rs.getString("description"));
                challenge.setDifficulty(rs.getString("difficulty"));
                challenge.setGithub(rs.getInt("github"));
                Timestamp deadlineTs = rs.getTimestamp("dead_line");
                if (deadlineTs != null) {
                    challenge.setDeadLine(deadlineTs.toLocalDateTime());
                }

                Group group = new Group();
                group.setId(rs.getInt("group_id_id"));
                group.setName(rs.getString("group_name"));

                Activity activity = new Activity();
                activity.setId(rs.getInt("activityId"));
                activity.setStatus(rs.getString("activity_status"));
                activity.setSubmissionFile(rs.getString("activity_submission"));

                activity.setChallenge(challenge);
                activity.setGroup(group);

                list.add(activity);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<OldActivityCardData> getOldActivityCardDataForUser(int userId) {
        List<OldActivityCardData> list = new ArrayList<>();
        String query = "SELECT DISTINCT " +
                "a.id AS activityId, " +
                "a.status AS activity_status, " +
                "a.group_id_id, " +
                "a.submission_file AS activity_submission, " +
                "g.name AS group_name, " +
                "c.id AS challenge_id, " +
                "c.title, " +
                "c.description, " +
                "c.difficulty, " +
                "c.dead_line, " +
                "c.github, " +
                "EXISTS(SELECT 1 FROM membership ml WHERE ml.group_id_id = a.group_id_id AND ml.user_id_id = ? AND LOWER(ml.role) = 'leader') AS is_leader, " +
                "EXISTS(SELECT 1 FROM evaluation e WHERE e.activity_id_id = a.id AND e.status IS NOT NULL) AS has_evaluation " +
                "FROM activity a " +
                "JOIN challenge c ON a.id_challenge_id = c.id " +
                "JOIN `group` g ON a.group_id_id = g.id " +
                "JOIN membership m ON m.group_id_id = a.group_id_id AND m.user_id_id = ? " +
                "WHERE a.status IN ('submitted', 'evaluated') " +
                "ORDER BY a.submission_date DESC";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Challenge challenge = new Challenge();
                challenge.setId(rs.getInt("challenge_id"));
                challenge.setTitle(rs.getString("title"));
                challenge.setDescription(rs.getString("description"));
                challenge.setDifficulty(rs.getString("difficulty"));
                challenge.setGithub(rs.getInt("github"));

                Timestamp deadlineTs = rs.getTimestamp("dead_line");
                if (deadlineTs != null) {
                    challenge.setDeadLine(deadlineTs.toLocalDateTime());
                }

                Group group = new Group();
                group.setId(rs.getInt("group_id_id"));
                group.setName(rs.getString("group_name"));

                Activity activity = new Activity();
                activity.setId(rs.getInt("activityId"));
                activity.setStatus(rs.getString("activity_status"));
                activity.setSubmissionFile(rs.getString("activity_submission"));
                activity.setChallenge(challenge);
                activity.setGroup(group);

                list.add(new OldActivityCardData(
                        activity,
                        rs.getBoolean("is_leader"),
                        rs.getBoolean("has_evaluation")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public static final class OldActivityCardData {
        private final Activity activity;
        private final boolean leader;
        private final boolean evaluation;

        public OldActivityCardData(Activity activity, boolean leader, boolean evaluation) {
            this.activity = activity;
            this.leader = leader;
            this.evaluation = evaluation;
        }

        public Activity getActivity() {
            return activity;
        }

        public boolean isLeader() {
            return leader;
        }

        public boolean hasEvaluation() {
            return evaluation;
        }
    }

    public void updateActivityFile(Activity a) {
        String query = "UPDATE activity SET submission_file = ? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, a.getSubmissionFile());
            ps.setInt(2, a.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(int activity_id) {
        String query = "DELETE FROM activity WHERE id=? ";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, activity_id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public List<Activity> displayAll() {
        String query = "Select * FROM activity WHERE submission_date is not null ";
        List<Activity> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Activity a = new Activity();

                a.setId(rs.getInt("id"));
                a.setSubmissionFile(rs.getString("submission_file"));

                a.setSubmissionDate(rs.getTimestamp("submission_date").toLocalDateTime());

                a.setStatus(rs.getString("status"));
                Challenge placeChallenge = new Challenge();
                placeChallenge.setId(rs.getInt("id_challenge_id"));
                a.setChallenge(placeChallenge);
                Group placeGroup = new Group();
                placeGroup.setId(rs.getInt("group_id_id"));
                a.setGroup(placeGroup);

                list.add(a);

            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public List<CalendarActivityData> getCalendarActivitiesForSupervisor(int supervisorId) {
        String query = "SELECT a.id, a.id_challenge_id, a.status, a.start_time, a.submission_date, c.title AS challenge_title, g.name AS group_name, " +
                "EXISTS(SELECT 1 FROM evaluation e WHERE e.activity_id_id = a.id AND LOWER(COALESCE(e.status, '')) = 'finished') AS evaluation_finished " +
                "FROM activity a " +
                "JOIN challenge c ON c.id = a.id_challenge_id " +
                "JOIN `group` g ON g.id = a.group_id_id " +
                "WHERE c.creator_id = ?";
        List<CalendarActivityData> activities = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, supervisorId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                CalendarActivityData data = new CalendarActivityData();
                data.setActivityId(rs.getInt("id"));
                data.setChallengeId(rs.getInt("id_challenge_id"));
                data.setChallengeTitle(rs.getString("challenge_title"));
                data.setGroupName(rs.getString("group_name"));
                data.setStatus(rs.getString("status"));

                Timestamp startTs = rs.getTimestamp("start_time");
                if (startTs != null) {
                    data.setStartTime(startTs.toLocalDateTime());
                }

                Timestamp submissionTs = rs.getTimestamp("submission_date");
                if (submissionTs != null) {
                    data.setSubmissionDate(submissionTs.toLocalDateTime());
                }

                data.setEvaluationFinished(rs.getBoolean("evaluation_finished"));
                activities.add(data);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load calendar activities", e);
        }

        return activities;
    }

    public static final class CalendarActivityData {
        private int activityId;
        private int challengeId;
        private String challengeTitle;
        private String groupName;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime submissionDate;
        private boolean evaluationFinished;

        public int getActivityId() {
            return activityId;
        }

        public void setActivityId(int activityId) {
            this.activityId = activityId;
        }

        public int getChallengeId() {
            return challengeId;
        }

        public void setChallengeId(int challengeId) {
            this.challengeId = challengeId;
        }

        public String getChallengeTitle() {
            return challengeTitle;
        }

        public void setChallengeTitle(String challengeTitle) {
            this.challengeTitle = challengeTitle;
        }

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getSubmissionDate() {
            return submissionDate;
        }

        public void setSubmissionDate(LocalDateTime submissionDate) {
            this.submissionDate = submissionDate;
        }

        public boolean isEvaluationFinished() {
            return evaluationFinished;
        }

        public void setEvaluationFinished(boolean evaluationFinished) {
            this.evaluationFinished = evaluationFinished;
        }
    }

    /*public boolean isActivitySubmitted(int activity_id){
        String query ="SELECT COUNT(*) from activity WHERE id=? AND status IN (?, ?)";
        try (PreparedStatement ps= connection.prepareStatement(query)){
            ps.setInt(1,activity_id);
            ps.setString(2,"submitted");
            ps.setString(3,"evaluated");
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                return rs.getInt(1)>0;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return false;
    }*/
    public PredictionInput getPredictionInput(int groupId, int challengeId) {
        String query = """
                SELECT
                    hist.avg_group_score,
                    hist.avg_completion_time,
                    target.difficulty,
                    target.deadline_days,
                    hist.group_skill_variance,
                    grp.group_size
                FROM
                (
                    SELECT
                        a.group_id_id AS group_id,
                        AVG(e.group_score) AS avg_group_score,
                        AVG(TIMESTAMPDIFF(SECOND, a.start_time, a.submission_date) / 86400.0) AS avg_completion_time,
                        AVG(COALESCE(ma_stats.skill_variance, 0)) AS group_skill_variance
                    FROM activity a
                    JOIN evaluation e ON e.activity_id_id = a.id
                    LEFT JOIN (
                        SELECT
                            ma.id_activity_id,
                            COALESCE(VAR_SAMP(ma.indiv_score), 0) AS skill_variance
                        FROM member_activity ma
                        GROUP BY ma.id_activity_id
                    ) ma_stats ON ma_stats.id_activity_id = a.id
                    WHERE a.group_id_id = ?
                      AND (a.status = 'submitted' OR a.status = 'evaluated')
                      AND a.id_challenge_id <> ?
                    GROUP BY a.group_id_id
                ) hist
                JOIN
                (
                    SELECT
                        c.id,
                        CASE
                            WHEN c.difficulty = 'Hard' THEN 2
                            WHEN c.difficulty = 'Medium' THEN 1
                            ELSE 0
                        END AS difficulty,
                        DATEDIFF(c.dead_line, c.created_at) AS deadline_days
                    FROM challenge c
                    WHERE c.id = ?
                ) target
                JOIN
                (
                    SELECT
                        m.group_id_id AS group_id,
                        COUNT(m.id) AS group_size
                    FROM membership m
                    WHERE m.group_id_id = ?
                    GROUP BY m.group_id_id
                ) grp ON grp.group_id = hist.group_id
                """;

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, groupId);
            ps.setInt(2, challengeId);
            ps.setInt(3, challengeId);
            ps.setInt(4, groupId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException(
                            "No historical submitted challenges found for group " + groupId
                    );
                }

                int deadlineDays = rs.getInt("deadline_days");
                if (deadlineDays <= 0) {
                    throw new IllegalStateException(
                            "Invalid deadline_days for challenge " + challengeId
                    );
                }
                System.out.println(
                        rs.getDouble("avg_group_score") + ", " +
                                rs.getDouble("avg_completion_time") + ", " +
                                rs.getInt("difficulty") + ", " +
                                deadlineDays + ", " +
                                rs.getDouble("group_skill_variance") + ", " +
                                rs.getInt("group_size")
                );

                return new PredictionInput(
                        rs.getDouble("avg_group_score"),
                        rs.getDouble("avg_completion_time"),
                        rs.getInt("difficulty"),
                        deadlineDays,
                        rs.getDouble("group_skill_variance"),
                        rs.getInt("group_size")
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load prediction input", e);
        }
    }

    public void updateGitUserName(User u, String git) {
        String query = "UPDATE user SET git_username=? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, git);
            ps.setInt(2, u.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getSupervisorGitUsername(int activityId) {
        String query = "SELECT u.git_username " +
                "FROM activity a " +
                "JOIN challenge c ON c.id = a.id_challenge_id " +
                "JOIN `user` u ON u.id = c.creator_id " +
                "WHERE a.id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("git_username");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public void markRepoCreated(int activityId) {
        String query = "UPDATE activity SET repo_created = 1 WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, activityId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }



}
