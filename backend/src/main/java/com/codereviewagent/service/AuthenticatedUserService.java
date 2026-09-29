package com.codereviewagent.service;

import com.codereviewagent.entity.User;

public interface AuthenticatedUserService {
    /**
     * Obtains the current authenticated user strictly from the server-side SecurityContext.
     * Throws UnauthorizedException (HTTP 401) if no valid user is authenticated.
     */
    User getCurrentUser();
}
