package com.gichungasoftwares.book_network.auth;

import com.gichungasoftwares.book_network.role.RoleRepository;
import com.gichungasoftwares.book_network.security.JwtService;
import com.gichungasoftwares.book_network.user.Token;
import com.gichungasoftwares.book_network.user.TokenRepository;
import com.gichungasoftwares.book_network.user.User;
import com.gichungasoftwares.book_network.user.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.misc.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;

    public void register(RegistrationRequest registrationRequest) throws MessagingException {

        var userRole = roleRepository.findByName("USER")
                // todo - better exception handling
                .orElseThrow(() -> new IllegalStateException("ROLE USER was not found!"));

        // create a user object
        var user = User.builder()
                .firstname(registrationRequest.getFirstname())
                .lastname(registrationRequest.getLastname())
                .email(registrationRequest.getEmail())
                .password(passwordEncoder.encode(registrationRequest.getPassword()))
                .isAccountLocked(false)
                .isEnabled(false)
                .roles(List.of(userRole))
                .build();

        userRepository.save(user);
        sendValidationEmail(user);
    }

    private void sendValidationEmail(User user) throws MessagingException {
        // generate and save activation token
        var newToken = generateAndSaveActivationToken(user);
        //send email
        emailService.sendEmail(
                user.getEmail(),
                user.fullName(),
                EmailTemplateName.ACTIVATE_ACCOUNT,
                activationUrl,
                newToken,
                "Account Activation"
        );
    }

    private String generateAndSaveActivationToken(User user) {
        // generate token
        String generatedToken = generateActivationCode(6);
        //save token
        var token = Token.builder()
                .token(generatedToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();
        System.out.println("printing token.........." + token);
        tokenRepository.save(token);
        return generatedToken;
    }

    // generate 6 digit activation code
    private String generateActivationCode(int length) {
        String characters = "0123456789"; // because we want to have number codes
        StringBuilder codeBuilder = new StringBuilder(); // to store the code
        SecureRandom secureRandom = new SecureRandom(); // ensure the generated value is cryptographically secure
        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(characters.length()); // 0..9
            codeBuilder.append(characters.charAt(randomIndex)); // store the character at this index to the codebuilder
        }
        return  codeBuilder.toString();
    }


    public AuthenticationResponse authenticate(AuthenticationRequest authRequest) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getEmail(),
                        authRequest.getPassword()
                )
        );
        var claims = new HashMap<String, Object>();
        var user = ((User)auth.getPrincipal());
        claims.put("fullName", user.fullName());
        var jwtToken = jwtService.generateToken(claims, user);
        return AuthenticationResponse.builder().token(jwtToken).build();
    }

//    @Transactional
    public void activateAccount(String token) throws MessagingException {
        // check if token exists in the db
        Token savedToken = tokenRepository.findByToken(token)
                // todo: exception has to be defined
                .orElseThrow(() -> new RuntimeException("Invalid token"));
        //
        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            sendValidationEmail(savedToken.getUser());
            throw new RuntimeException("Activation token has expired> A new token has been sent to your email address");
        }
        var user = userRepository.findById(savedToken.getUser().getUser_id())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);
        savedToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(savedToken);
    }
}



























