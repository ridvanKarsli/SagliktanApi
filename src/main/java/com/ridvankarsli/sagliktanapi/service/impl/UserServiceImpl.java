package com.ridvankarsli.sagliktanapi.service.impl;

import com.ridvankarsli.sagliktanapi.domain.ReactionValue;
import com.ridvankarsli.sagliktanapi.domain.User;
import com.ridvankarsli.sagliktanapi.exception.ResourceNotFoundException;
import com.ridvankarsli.sagliktanapi.repository.CommentRepository;
import com.ridvankarsli.sagliktanapi.repository.PostRepository;
import com.ridvankarsli.sagliktanapi.repository.ReactionRepository;
import com.ridvankarsli.sagliktanapi.repository.UserRepository;
import com.ridvankarsli.sagliktanapi.service.UserService;
import com.ridvankarsli.sagliktanapi.util.SearchQueryUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;

    @Override
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, String firstName, String lastName, String bio) {
        User user = getById(userId);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setBio(bio);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivate(Long userId) {
        User user = getById(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    public Page<User> search(String query, Pageable pageable) {
        String tsQuery = SearchQueryUtil.toPrefixTsQuery(query);
        if (tsQuery == null) {
            return Page.empty(pageable);
        }
        return userRepository.search(query, tsQuery, SearchQueryUtil.stripSort(pageable));
    }

    @Override
    public ProfileStats getProfileStats(Long userId) {
        long postCount = postRepository.countByUserId(userId);
        long commentCount = commentRepository.countByUserIdAndDeletedFalse(userId);
        long likes = 0;
        long dislikes = 0;
        for (ReactionRepository.ReceivedReactionCountRow row : reactionRepository.countReceivedByUserId(userId)) {
            if (ReactionValue.HELPFUL.name().equals(row.getValue())) {
                likes = row.getCount();
            } else if (ReactionValue.NOT_HELPFUL.name().equals(row.getValue())) {
                dislikes = row.getCount();
            }
        }
        return new ProfileStats(postCount, commentCount, likes, dislikes);
    }
}
