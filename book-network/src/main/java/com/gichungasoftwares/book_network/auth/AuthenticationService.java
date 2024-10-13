package com.gichungasoftwares.book_network.auth;

import com.gichungasoftwares.book_network.role.RoleRepository;
import com.gichungasoftwares.book_network.security.JwtService;
import com.gichungasoftwares.book_network.user.Token;
import com.gichungasoftwares.book_network.user.TokenRepository;
import com.gichungasoftwares.book_network.user.User;
import com.gichungasoftwares.book_network.user.UserRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
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
    @Value("${application.account.activation.token.expiration}")
    private byte activationTokenValidity;


    public void register(RegistrationRequest regRequest) throws MessagingException {
        // check if role user exists before creating a user
        var userRole = roleRepository.findByName("USER")
                // todo - better exception handling
                .orElseThrow(() -> new IllegalStateException("ROLE USER was not found!"));

        // check if email is already in use
        if (userRepository.existsByEmail(regRequest.getEmail())) {
            throw new MessagingException("Error: Email is already in use!");
        }

        // create a user object
        var user = User.builder()
                .firstname(regRequest.getFirstname())
                .lastname(regRequest.getLastname())
                .email(regRequest.getEmail())
                .password(passwordEncoder.encode(regRequest.getPassword()))
                .isAccountLocked(false)
                .isEnabled(false)
                .roles(List.of(userRole))
                .build();

        // save the user
        userRepository.save(user);

        // send verification email to the saved user
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
        var activationToken = Token.builder()
                .token(generatedToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(activationTokenValidity))
                .user(user)
                .build();
        System.out.println("printing token.........." + activationToken);
        tokenRepository.save(activationToken);
        return generatedToken;
    }


    // generate 6 digit activation code
    private String generateActivationCode(int length) {
        String characters = "0123456789"; // because we want to have number activation code codes
        StringBuilder codeBuilder = new StringBuilder(); // to store the code
        SecureRandom secureRandom = new SecureRandom(); // ensure the generated value is cryptographically secure
        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(characters.length()); // 0..9
            codeBuilder.append(characters.charAt(randomIndex)); // store the character at this index to the codeBuilder
        }
        return  codeBuilder.toString(); // convert the generated code to string
    }


    // sign in user
    public LoginResponse login(LoginRequest loginRequest) {
        // let spring security handle the authentication process including checking password matching
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        var claims = new HashMap<String, Object>();
        var user = ((User)auth.getPrincipal()); // cast the authenticated principal to User object
        claims.put("fullName", user.fullName());

        var jwtToken = jwtService.generateToken(claims, user);
        return LoginResponse.builder().token(jwtToken).build();
    }


    // @Transactional
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



























