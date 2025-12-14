package xyz.oiio.n8n.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.oiio.n8n.repository.UserRepository;

import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

        private final UserRepository userRepository;

        @Override
        @Transactional(readOnly = true, noRollbackFor = UsernameNotFoundException.class)
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
                xyz.oiio.n8n.entity.User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "User not found with email: " + email));

                log.info("Found user: {} with id: {} and password hash: {}", email, user.getId(), user.getPassword());

                log.info("Loading user: {}", email);

                return new User(
                                user.getId(),
                                user.getPassword(),
                                user.getDisabled() == null || !user.getDisabled(),
                                true,
                                true,
                                user.getMfaEnabled() == null || !user.getMfaEnabled(),
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        }

        @Transactional(readOnly = true, noRollbackFor = UsernameNotFoundException.class)
        public UserDetails loadUserById(String userId) throws UsernameNotFoundException {
                xyz.oiio.n8n.entity.User user = userRepository.findById(userId)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

                return new User(
                                user.getId(),
                                user.getPassword(),
                                user.getDisabled() == null || !user.getDisabled(),
                                true,
                                true,
                                user.getMfaEnabled() == null || !user.getMfaEnabled(),
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        }
}