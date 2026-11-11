package com.example.ruta705.core.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Contrato HTTP con el backend de Ruta705 (`/api/v1`). Ver docs/API.md del backend. */
interface Ruta705Api {
    // Meta
    @GET("meta")
    suspend fun meta(): Envelope<AppMeta>

    // Autenticación
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Envelope<AuthSession>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Envelope<AuthSession>

    @POST("auth/google")
    suspend fun google(@Body body: GoogleRequest): Envelope<AuthSession>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Envelope<AuthSession>

    @POST("auth/logout")
    suspend fun logout(@Body body: RefreshRequest): Response<Unit>

    @POST("auth/password/forgot")
    suspend fun forgotPassword(@Body body: ForgotRequest): Envelope<MessageResponse>

    @POST("auth/password/reset")
    suspend fun resetPassword(@Body body: ResetRequest): Response<Unit>

    // Perfil
    @GET("me")
    suspend fun me(): Envelope<User>

    @PATCH("me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): Envelope<User>

    @POST("me/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Envelope<AuthSession>

    @DELETE("me")
    suspend fun deleteAccount(): Response<Unit>

    // Rutas y planificador
    @GET("routes")
    suspend fun routes(@Query("q") query: String? = null): Envelope<List<RouteSummary>>

    @GET("routes/{id}")
    suspend fun route(@Path("id") id: String): Envelope<RouteDetail>

    @GET("stops")
    suspend fun stops(
        @Query("q") query: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radius") radius: Int? = null,
        @Query("limit") limit: Int? = null,
    ): Envelope<List<StopItem>>

    @POST("planner/search")
    suspend fun plan(@Body body: PlanRequest): Envelope<PlanResult>

    @GET("geocode/search")
    suspend fun geocode(@Query("q") query: String): Envelope<List<GeoPlace>>

    @GET("geocode/reverse")
    suspend fun reverseGeocode(@Query("lat") lat: Double, @Query("lng") lng: Double): Envelope<GeoPlace?>

    // Reportes
    @GET("reports")
    suspend fun reports(
        @Query("routeIds") routeIds: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): Page<Report>

    @POST("reports")
    suspend fun createReport(@Body body: CreateReportRequest): CreateReportResponse

    @POST("reports/{id}/vote")
    suspend fun vote(@Path("id") id: String, @Body body: VoteRequest): Envelope<Report>

    @POST("reports/{id}/flag")
    suspend fun flag(@Path("id") id: String, @Body body: FlagRequest): Response<Unit>

    @DELETE("reports/{id}")
    suspend fun deleteReport(@Path("id") id: String): Response<Unit>

    @GET("announcements")
    suspend fun announcements(@Query("routeId") routeId: String? = null): Envelope<List<Announcement>>

    // Favoritos
    @GET("me/favorites/routes")
    suspend fun favoriteRoutes(): Envelope<List<FavoriteRoute>>

    @PUT("me/favorites/routes/{routeId}")
    suspend fun addFavoriteRoute(@Path("routeId") routeId: String): Response<Unit>

    @PATCH("me/favorites/routes/{routeId}")
    suspend fun setFavoriteNotify(@Path("routeId") routeId: String, @Body body: NotifyRequest): Response<Unit>

    @DELETE("me/favorites/routes/{routeId}")
    suspend fun removeFavoriteRoute(@Path("routeId") routeId: String): Response<Unit>

    @GET("me/favorites/trips")
    suspend fun trips(): Envelope<List<SavedTrip>>

    @POST("me/favorites/trips")
    suspend fun saveTrip(@Body body: SaveTripRequest): Envelope<SavedTrip>

    @DELETE("me/favorites/trips/{id}")
    suspend fun deleteTrip(@Path("id") id: String): Response<Unit>

    // Notificaciones
    @GET("me/notifications")
    suspend fun notifications(@Query("cursor") cursor: String? = null, @Query("limit") limit: Int = 20): Page<AppNotification>

    @GET("me/notifications/unread-count")
    suspend fun unreadCount(): Envelope<UnreadCount>

    @POST("me/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String): Response<Unit>

    @POST("me/notifications/read-all")
    suspend fun markAllRead(): Response<Unit>

    @GET("me/notification-preferences")
    suspend fun notificationPrefs(): Envelope<NotificationPrefs>

    @PUT("me/notification-preferences")
    suspend fun updateNotificationPrefs(@Body body: NotificationPrefs): Envelope<NotificationPrefs>
}
