package com.demo.springsecurityclient.controller;

import com.demo.springsecurityclient.entity.User;
import com.demo.springsecurityclient.entity.VerificationToken;
import com.demo.springsecurityclient.event.RegistrationCompleteEvent;
import com.demo.springsecurityclient.model.PasswordModel;
import com.demo.springsecurityclient.model.UserModel;
import com.demo.springsecurityclient.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@RestController
@Slf4j
public class RegistrationController {


    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private WebClient webClient;

    @GetMapping("/api/users")
    public String[] users(@RegisteredOAuth2AuthorizedClient("api-client-authorization-code")OAuth2AuthorizedClient client){
        return this.webClient
                .get()
                .uri("http://127.0.0.1:8090/api/users")
                .attributes(oauth2AuthorizedClient(client))
                .retrieve()
                .bodyToMono(String[].class)
                .block();
    }

    @GetMapping("/api/hello")
    public String hello(Principal principal) {
        return "Hello " +principal.getName()+", Welcome to World War!!";
    }

    @PostMapping("/register") //main API flow
    public String registerUser(@RequestBody UserModel userModel, final HttpServletRequest request){
        User user = userService.registerUser(userModel);
        publisher.publishEvent(new RegistrationCompleteEvent(user, applicationUrl(request)));

        return "Success";
    }

    @GetMapping("/verifyRegistration")
    public String verifyRegistration(@RequestParam("token") String token){

        String result = userService.validateVerificationToken(token);
        if(result.equalsIgnoreCase("valid")){
            return "User verifies Successfully";
        }

        return "Bad User";
    }

    @GetMapping("/resendVerifyToken")
    public String resendVerficationToken(@RequestParam("token") String oldToken, HttpServletRequest request){
        VerificationToken verificationToken=userService.generateNewVerificationToken(oldToken);

        User user=verificationToken.getUser();
        resendVerificationTokenMail(user, applicationUrl(request), verificationToken);

        return "Verifcation Link Sent";
    }

    private void resendVerificationTokenMail(User user, String applicationUrl, VerificationToken verificationToken) {

        String url= applicationUrl
                +"/verifyRegistration?token="
                +verificationToken.getToken();

        //sendVerificationEmail()
        log.info("Click the link to verify your account: {}", url);

    }

    @PostMapping("/resetPassword")
    public String resetPassword(@RequestBody PasswordModel passwordModel, HttpServletRequest request){
        User user=userService.findUserByEmail(passwordModel.getEmail());
        String url="";

        if(user!=null){
            String token= UUID.randomUUID().toString();
            userService.createPasswordResetTokenForUser(user,token);
            url=passwordResetTokenMail(user,applicationUrl(request), token);
        }


        return url;
    }

    @PostMapping("/savePassword")
    public String savePassword(@RequestParam("token") String token, @RequestBody PasswordModel passwordModel){
        String result=userService.validatePasswordResetToken(token);
        if(!result.equalsIgnoreCase("valid")){
            return "Invalid Token";
        }
        Optional<User> user=userService.getUserByPasswordResetToken(token);
        if(user.isPresent()){
            userService.changePassword(user.get(),passwordModel.getNewPassword());
            return "Password Reset Successfully";
        } else {
            return "Invalid Token";
        }
    }

    @PostMapping("/changePassword")
    public String changePassword(@RequestBody PasswordModel passwordModel){
        User user=userService.findUserByEmail(passwordModel.getEmail());
        if(!userService.checkIfValidOldPassword(user,passwordModel.getOldPassword())){
            return "Invalid Old Password";
        }

        //Save new password
        userService.changePassword(user,passwordModel.getNewPassword());

        return "Password Changed Successfully";
    }

    private String passwordResetTokenMail(User user, String applicationUrl, String token) {

        String url= applicationUrl
                +"/savePassword?token="
                +token;

        //sendVerificationEmail()
        log.info("Click the link to verify your account: {}", url);

        return url;

    }

    private String applicationUrl(HttpServletRequest request) {
        return "http://" +
                request.getServerName() +
                ":" +
                request.getServerPort() +
                request.getContextPath();
    }

}
