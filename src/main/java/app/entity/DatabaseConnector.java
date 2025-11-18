package app.entity;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class DatabaseConnector {
    private static Connection conn;
    private static final String URL = System.getenv("DATABASE_URL");
    private static final String USER = System.getenv("DATABASE_USER");
    private static final String PASSWORD = System.getenv("DATABASE_PASSWORD");
    private static final String ACCOUNT_ID_STRING = "account_id";
    private static final String USERNAME_STRING = "username";
    private static final String PROFILE_PICTURE_URL_STRING = "profile_picture_url";

    private DatabaseConnector() {
    }

    static {
        conn = openConnection();
    }

    public static synchronized boolean ensureConnection() {
        if (isConnected()) {
            return true;
        }
        closeQuietly();
        conn = openConnection();
        return conn != null;
    }

    private static boolean isConnected() {
        if (conn == null) {
            return false;
        }
        try {
            return !conn.isClosed();
        } catch (SQLException _) {
            return false;
        }
    }

    private static Connection openConnection() {
        try {
            return createConnection();
        } catch (SQLException _) {
            return null;
        }
    }

    private static void closeQuietly() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException _) {
                // Ignore
            } finally {
                conn = null;
            }
        }
    }

    private static Connection createConnection() throws SQLException {
        String dbUrl = URL;
        String dbUser = USER;
        String dbPassword = PASSWORD;

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream("C:\\SchoolProjects\\keys.txt")) {
                props.load(fis);
                dbUrl = props.getProperty("url");
                dbUser = props.getProperty("user");
                dbPassword = props.getProperty("password");
            } catch (IOException e) {
                throw new SQLException("Failed to load database credentials", e);
            }
        }
        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new SQLException("Database credentials are not configured.");
        }
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public static Profile getFullProfile(Profile profileSkeleton) throws SQLException {
        Profile fullProfile = getProfileByUsername(profileSkeleton.getUsername());
        if (fullProfile == null) {
            return profileSkeleton;
        }
        fullProfile.setArtistPosts(loadPostsForAccount(fullProfile.getAccountId()));
        return fullProfile;
    }

    public static Post addArtistPost(int accountId, String caption, String imageUrl, String keywords)
            throws SQLException {
        if (accountId <= 0) {
            throw new SQLException("Account id must be positive");
        }
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new SQLException("Image URL is required");
        }
        if (!ensureConnection()) {
            throw new SQLException("Unable to obtain database connection");
        }
        String sql = """
                INSERT INTO Posts2 (account_id, caption, post_picture_url, keywords)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, accountId);
            stmt.setString(2, caption);
            stmt.setString(3, imageUrl);
            stmt.setString(4, keywords);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int postId = keys.getInt(1);
                    return new Post(postId, caption, imageUrl, accountId, keywords);
                }
            }
        }
        throw new SQLException("Failed to create post record");
    }

    public static Account getAccountByUsername(String queryUsername) throws SQLException {
        PreparedStatement stmt = DatabaseConnector.conn.prepareStatement("""
                SELECT *
                FROM Accounts AS A
                LEFT JOIN AccountStylePreferences AS ASP ON ASP.account_id = A.account_id
                WHERE A.username = ?;
                """);
        stmt.setString(1, queryUsername);
        ResultSet rs = stmt.executeQuery();
        return convertToAccount(rs);
    }

    private static Account convertToAccount(ResultSet rs) throws SQLException {
        if (!rs.next()) {
            return null;
        }
        List<String> stylesList = new ArrayList<>();
        int id = rs.getInt(ACCOUNT_ID_STRING);
        String username = rs.getString(USERNAME_STRING);
        String password = rs.getString("password");
        String profilePictureUrl = rs.getString(PROFILE_PICTURE_URL_STRING);
        double homeLatitude = rs.getDouble("home_latitude");
        double homeLongitude = rs.getDouble("home_longitude");
        String style = rs.getString("style_name");
        do {
            stylesList.add(style);
        } while (rs.next());
        return new Account(id, username, password, profilePictureUrl, homeLatitude, homeLongitude, stylesList);
    }

    public static Profile getProfileByUsername(String queryUsername) throws SQLException {
        PreparedStatement profileQueryStatement = DatabaseConnector.conn.prepareStatement("""
                SELECT *
                  FROM Artists AS A
                  LEFT JOIN ArtistTaggedStyles AS ATS ON ATS.artist_id = A.artist_id
                  LEFT JOIN Accounts AS Acc ON Acc.account_id = A.account_id
                 WHERE username = ? ;
                """);
        profileQueryStatement.setString(1, queryUsername);
        ResultSet rs = profileQueryStatement.executeQuery();
        List<Profile> artistProfilesList = convertToArtistProfiles(rs);
        Profile profile = artistProfilesList.isEmpty() ? null : artistProfilesList.getFirst();
        if (profile != null) {
            profile.setArtistPosts(loadPostsForAccount(profile.getAccountId()));
        }
        return profile;
    }

    private static List<Profile> convertToArtistProfiles(ResultSet rs) throws SQLException {
        if (!rs.next()) {
            return Collections.emptyList();
        }
        List<Profile> artistProfiles = new ArrayList<>();
        while (!rs.isAfterLast()) {
            int accountId = rs.getInt(ACCOUNT_ID_STRING);
            String username = rs.getString(USERNAME_STRING);
            String profilePictureUrl = rs.getString(PROFILE_PICTURE_URL_STRING);
            String biography = rs.getString("biography");
            String workAddress = rs.getString("work_address");
            double workLongitude = rs.getDouble("work_longitude");
            double workLatitude = rs.getDouble("work_latitude");

            List<String> styles = new ArrayList<>();
            String style = rs.getString("style_name");
            if (style != null) {
                styles.add(style);
            }
            while (rs.next()) {
                if (rs.getInt(ACCOUNT_ID_STRING) != accountId) {
                    break;
                }
                styles.add(style);
            }
            Profile.WorkLocation location = new Profile.WorkLocation(workAddress, workLongitude, workLatitude);
            artistProfiles.add(new Profile(accountId, username, profilePictureUrl, biography, styles, location));
        }
        return artistProfiles;

    }

    public static List<Profile> getProfilesLike(String pattern) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("""
                SELECT *
                  FROM Artists AS A
                  LEFT JOIN ArtistTaggedStyles AS ATS ON ATS.artist_id = A.artist_id
                  LEFT JOIN Accounts AS ACC ON ACC.account_id = A.account_id
                 WHERE username LIKE ?
                """);
        stmt.setString(1, "%" + pattern + "%");
        ResultSet rs = stmt.executeQuery();
        return convertToArtistProfiles(rs);
    }

    public static List<Profile> getProfilesWithinBounds(double latitudeFrom, double latitudeTo, double longitudeFrom,
            double longitudeTo) throws SQLException {
        PreparedStatement profileQueryStatement = DatabaseConnector.conn.prepareStatement(
                """
                        SELECT *
                          FROM Artists AS A
                          LEFT JOIN ArtistTaggedStyles AS ATS ON ATS.artist_id = A.artist_id
                          LEFT JOIN Accounts AS ACC ON ACC.account_id = A.account_id
                         WHERE A.work_latitude >= ? AND A.work_latitude <= ? AND A.work_longitude >= ? AND A.work_longitude <= ?;
                        """);
        profileQueryStatement.setDouble(1, latitudeFrom);
        profileQueryStatement.setDouble(2, latitudeTo);
        profileQueryStatement.setDouble(3, longitudeFrom);
        profileQueryStatement.setDouble(4, longitudeTo);
        ResultSet rs = profileQueryStatement.executeQuery();
        return convertToArtistProfiles(rs);
    }

    public static void createUser(String username, String password, boolean isArtist) throws SQLException {
        PreparedStatement insertAccountsStmt = DatabaseConnector.conn.prepareStatement("""
                INSERT INTO Accounts(username, password)
                VALUES (?, ?);
                """, Statement.RETURN_GENERATED_KEYS);
        insertAccountsStmt.setString(1, username);
        insertAccountsStmt.setString(2, password);
        insertAccountsStmt.executeUpdate();

        ResultSet rs = insertAccountsStmt.getGeneratedKeys();
        if (rs.next()) {
            int userId = rs.getInt(1);
            if (isArtist) {
                PreparedStatement insertArtistsStmt = DatabaseConnector.conn.prepareStatement("""
                        INSERT INTO Artists(account_id)
                        VALUES (?);
                        """);
                insertArtistsStmt.setInt(1, userId);
                insertArtistsStmt.executeUpdate();
            }
        }
    }

    private static List<Post> loadPostsForAccount(int accountId) throws SQLException {
        if (accountId <= 0) {
            return Collections.emptyList();
        }
        if (!ensureConnection()) {
            throw new SQLException("Unable to obtain database connection");
        }
        String sql = """
                SELECT post_id, account_id, caption, post_picture_url, keywords
                  FROM Posts2
                 WHERE account_id = ?
                 ORDER BY post_id DESC
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Post> posts = new ArrayList<>();
                while (rs.next()) {
                    posts.add(new Post(
                            rs.getInt("post_id"),
                            rs.getString("caption"),
                            rs.getString("post_picture_url"),
                            rs.getInt(ACCOUNT_ID_STRING),
                            rs.getString("keywords")));
                }
                return posts;
            }
        }
    }

    public static void modifyUser(Profile p) throws SQLException {
        String username = p.getUsername();
        String bio = p.getBiography();
        double longitude = p.getWorkLongitude();
        double lattitude = p.getWorkLatitude();
        String profilePictureUrl = p.getProfilePictureURL();

        if (username == null || username.isBlank()) {
            throw new SQLException("Username is required to modify user data");
        }
        if (!ensureConnection()) {
            throw new SQLException("Unable to obtain database connection");
        }

        int accountId;
        try (PreparedStatement findAccount = conn
                .prepareStatement("SELECT account_id FROM Accounts WHERE username = ?")) {
            findAccount.setString(1, username);
            try (ResultSet rs = findAccount.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Account not found for username: " + username);
                }
                accountId = rs.getInt(ACCOUNT_ID_STRING);
            }
        }

        boolean previousAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try (PreparedStatement updateArtist = conn.prepareStatement("""
                    UPDATE Artists
                       SET biography = ?, work_latitude = ?, work_longitude = ?
                     WHERE account_id = ?
                """);
                PreparedStatement updateAccount = conn.prepareStatement("""
                            UPDATE Accounts
                               SET profile_picture_url = ?
                             WHERE account_id = ?
                        """)) {

            updateArtist.setString(1, bio);
            updateArtist.setDouble(2, lattitude);
            updateArtist.setDouble(3, longitude);
            updateArtist.setInt(4, accountId);
            updateArtist.executeUpdate();

            updateAccount.setString(1, profilePictureUrl);
            updateAccount.setInt(2, accountId);
            updateAccount.executeUpdate();

            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(previousAutoCommit);
        }
    }

    public static Review submitReview(int reviewerId, int revieweeId, String pictureUrl, String reviewText, int rating)
            throws SQLException {
        if (reviewerId <= 0 || revieweeId <= 0) {
            throw new SQLException("Reviewer and reviewee ids must be positive");
        }
        if (rating < 0 || rating > 5) {
            throw new SQLException("Rating must be between 0 and 5");
        }
        if (reviewText == null || reviewText.isBlank()) {
            throw new SQLException("Review text is required");
        }
        if (!ensureConnection()) {
            throw new SQLException("Unable to obtain database connection");
        }

        String sql = """
                INSERT INTO Reviews (reviewer_id, reviewee_id, review_text, rating, review_picture_url)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, reviewerId);
            stmt.setInt(2, revieweeId);
            stmt.setString(3, reviewText);
            stmt.setInt(4, rating);
            stmt.setString(5, pictureUrl);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    Review.Reviewer reviewer = new Review.Reviewer(reviewerId, null, null);
                    return new Review(keys.getInt(1), revieweeId, pictureUrl, reviewText, rating, reviewer);
                }
            }
        }
        throw new SQLException("Failed to create review record");
    }

    public static List<Review> loadReviews(int accountId) throws SQLException {
        if (accountId <= 0) {
            throw new SQLException("Account id must be positive");
        }
        if (!ensureConnection()) {
            throw new SQLException("Unable to obtain database connection");
        }
        String sql = """
                SELECT r.review_id,
                       r.reviewer_id,
                       r.reviewee_id,
                       r.review_text,
                       r.rating,
                       r.review_picture_url,
                       acc.username,
                       acc.profile_picture_url
                  FROM Reviews r
                  JOIN Artists a ON a.artist_id = r.reviewee_id
                  JOIN Accounts acc ON acc.account_id = r.reviewer_id
                 WHERE a.account_id = ?
                 ORDER BY r.review_id DESC
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Review> reviews = new ArrayList<>();
                while (rs.next()) {
                    Review.Reviewer reviewer = new Review.Reviewer(
                            rs.getInt("reviewer_id"),
                            rs.getString(USERNAME_STRING),
                            rs.getString(PROFILE_PICTURE_URL_STRING));
                    reviews.add(new Review(
                            rs.getInt("review_id"),
                            rs.getInt("reviewee_id"),
                            rs.getString("review_picture_url"),
                            rs.getString("review_text"),
                            rs.getInt("rating"),
                            reviewer));
                }
                return reviews;
            }
        }
    }
}
