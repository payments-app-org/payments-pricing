package com.paymentsapp.pricing.client;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

@Configuration
@ImportHttpServices(group = Backends.ACCOUNT, types = AccountApi.class)
@ImportHttpServices(group = Backends.BILLER, types = BillerApi.class)
class HttpClientsConfig {
}
