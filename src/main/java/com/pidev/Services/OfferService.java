package com.pidev.Services;


import com.pidev.models.Offer;
import com.pidev.models.User;
import com.pidev.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class OfferService {
    private final Connection connection;

    public OfferService() {
        this.connection = DataSource.getInstance().getConnection();
        if (this.connection == null) {
            throw new IllegalStateException("Connexion MySQL indisponible");
        }
    }

    public Offer ajouter(Offer offer) throws SQLException {
        validateOffer(offer, false);

        String sql = "INSERT INTO `offer` (entreprise_id, title, description, offer_type, `field`, required_level, "
                + "required_skills, location, contract_type, duration, salary_range, `status`, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillOfferStatement(preparedStatement, offer);
            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Insertion de l'offre échouée");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    offer.setId(generatedKeys.getInt(1));
                }
            }
        }

        return offer;
    }

    public List<Offer> afficher() throws SQLException {
        String sql = "SELECT id, entreprise_id, title, description, offer_type, `field`, required_level, required_skills, "
                + "location, contract_type, duration, salary_range, `status`, created_at FROM `offer` ORDER BY id DESC";
        List<Offer> offers = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                offers.add(mapOffer(resultSet));
            }
        }

        return offers;
    }

    public boolean modifier(Offer offer) throws SQLException {
        validateOffer(offer, true);

        String sql = "UPDATE `offer` SET entreprise_id = ?, title = ?, description = ?, offer_type = ?, `field` = ?, "
                + "required_level = ?, required_skills = ?, location = ?, contract_type = ?, duration = ?, "
                + "salary_range = ?, `status` = ?, created_at = ? WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            fillOfferStatement(preparedStatement, offer);
            preparedStatement.setInt(14, offer.getId());
            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean supprimer(int id) throws SQLException {
        validateId(id, "ID de l'offre invalide");

        String sql = "DELETE FROM `offer` WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    private void fillOfferStatement(PreparedStatement preparedStatement, Offer offer) throws SQLException {
        preparedStatement.setInt(1, offer.getEntreprise().getId());
        preparedStatement.setString(2, normalize(offer.getTitle()));
        preparedStatement.setString(3, normalize(offer.getDescription()));
        preparedStatement.setString(4, normalize(offer.getOfferType()));
        preparedStatement.setString(5, normalize(offer.getField()));
        preparedStatement.setString(6, normalize(offer.getRequiredLevel()));
        preparedStatement.setString(7, normalize(offer.getRequiredSkills()));
        preparedStatement.setString(8, normalize(offer.getLocation()));
        preparedStatement.setString(9, normalize(offer.getContractType()));
        setNullableInteger(preparedStatement, 10, offer.getDuration());
        setNullableDouble(preparedStatement, 11, offer.getSalaryRange());
        preparedStatement.setString(12, normalize(offer.getStatus()));
        preparedStatement.setTimestamp(13, Timestamp.valueOf(offer.getCreatedAt()));
    }

    private Offer mapOffer(ResultSet resultSet) throws SQLException {
        Offer offer = new Offer();
        offer.setId(resultSet.getInt("id"));

        Integer entrepriseId = getNullableInteger(resultSet, "entreprise_id");
        if (entrepriseId != null) {
            offer.setEntreprise(new User(entrepriseId));
        }

        offer.setTitle(resultSet.getString("title"));
        offer.setDescription(resultSet.getString("description"));
        offer.setOfferType(resultSet.getString("offer_type"));
        offer.setField(resultSet.getString("field"));
        offer.setRequiredLevel(resultSet.getString("required_level"));
        offer.setRequiredSkills(resultSet.getString("required_skills"));
        offer.setLocation(resultSet.getString("location"));
        offer.setContractType(resultSet.getString("contract_type"));
        offer.setDuration(getNullableInteger(resultSet, "duration"));
        offer.setSalaryRange(getNullableDouble(resultSet, "salary_range"));
        offer.setStatus(resultSet.getString("status"));
        offer.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        return offer;
    }

    private void validateOffer(Offer offer, boolean requireId) {
        if (offer == null) {
            throw new IllegalArgumentException("L'offre est obligatoire");
        }

        if (requireId) {
            validateId(offer.getId(), "L'ID de l'offre est obligatoire");
        }

        if (offer.getEntreprise() == null || offer.getEntreprise().getId() == null || offer.getEntreprise().getId() <= 0) {
            throw new IllegalArgumentException("L'entreprise est obligatoire");
        }

        validateRequiredString(offer.getTitle(), "Le titre est obligatoire", 2, 30, "Le titre doit contenir entre 2 et 30 caractères");
        validateRequiredString(offer.getDescription(), "La description est obligatoire", 10, 10000, "La description doit contenir au moins 10 caractères");
        validateRequiredString(offer.getOfferType(), "Le type d'offre est obligatoire", 2, 30, "Le type d'offre doit contenir entre 2 et 30 caractères");
        validateRequiredString(offer.getField(), "Le domaine est obligatoire", 2, 30, "Le domaine doit contenir entre 2 et 30 caractères");
        validateRequiredString(offer.getRequiredLevel(), "Le niveau requis est obligatoire", 2, 30, "Le niveau requis doit contenir entre 2 et 30 caractères");
        validateRequiredString(offer.getRequiredSkills(), "Les compétences requises sont obligatoires", 2, 10000, "Les compétences requises doivent être renseignées");
        validateRequiredString(offer.getLocation(), "La localisation est obligatoire", 2, 40, "La localisation doit contenir entre 2 et 40 caractères");
        validateRequiredString(offer.getContractType(), "Le type de contrat est obligatoire", 2, 40, "Le type de contrat doit contenir entre 2 et 40 caractères");
        validateRequiredString(offer.getStatus(), "Le statut est obligatoire", 2, 30, "Le statut doit contenir entre 2 et 30 caractères");

        if (offer.getDuration() != null && offer.getDuration() <= 0) {
            throw new IllegalArgumentException("La durée doit être positive");
        }

        if (offer.getSalaryRange() != null && offer.getSalaryRange() < 0) {
            throw new IllegalArgumentException("Le salaire doit être positif");
        }

        if (offer.getCreatedAt() == null) {
            throw new IllegalArgumentException("La date de création est obligatoire");
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

    private void setNullableDouble(PreparedStatement preparedStatement, int index, Double value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, Types.DOUBLE);
        } else {
            preparedStatement.setDouble(index, value);
        }
    }

    private Integer getNullableInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private Double getNullableDouble(ResultSet resultSet, String column) throws SQLException {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? null : value;
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
