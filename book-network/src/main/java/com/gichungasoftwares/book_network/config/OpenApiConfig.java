package com.gichungasoftwares.book_network.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(
                contact = @Contact(
                        name = "S.Gichunga",
                        email = "hello@sgichunga.com",
                        url = "https://sgichunga.com/courses"
                ),
                description = "OpenApi documentation for Spring Security",
                title = "OpenApi specification - S.Gichunga",
                version = "1.0",
                license = @License(
                        name = "License name",
                        url = "https://license-url.com"
                ),
                termsOfService = "Terms of Service"
        ),
        servers = {
                @Server(
                        description = "Local ENV",
                        url = "http://localhost:8088/api/v1"
                ),
                @Server(
                        description = "Prod ENV",
                        url = "https://sgichunga.com/courses"
                )
        },
        security = {
                @SecurityRequirement(
                        name = "bearerAuth"
                )
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT auth descritption",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig { }
