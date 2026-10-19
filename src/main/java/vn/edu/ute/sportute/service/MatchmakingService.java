package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.model.MatchmakingPost;
import java.util.List;

public interface MatchmakingService {

    List<MatchmakingPost> getAllPosts();

    List<MatchmakingPost> getActivePosts(String sport, String date);

    MatchmakingPost getPostById(String id);

    MatchmakingPost createPost(MatchmakingPost post);

    MatchmakingPost acceptMatch(String postId, String opponentName, String opponentPhone, String opponentEmail, String message);

    boolean reportViolation(String postId, String reason, String reporterName);

    boolean cancelPost(String postId, String requester);

    boolean cancelPostByBookingId(String bookingId, String requester);
}
