package com.example.ruta705.core.network

import kotlinx.serialization.Serializable

// ---------- Envolturas y errores ----------

@Serializable
data class Envelope<T>(val data: T)

@Serializable
data class PageMeta(val nextCursor: String? = null)

@Serializable
data class Page<T>(val data: List<T>, val meta: PageMeta = PageMeta())

@Serializable
data class ApiErrorBody(val error: ApiErrorDetail)

@Serializable
data class ApiErrorDetail(val code: String, val message: String, val details: List<FieldError>? = null)

@Serializable
data class FieldError(val path: String? = null, val message: String)

// ---------- Autenticación ----------

@Serializable
data class User(
    val id: String,
    val email: String,
    val phone: String? = null,
    val fullName: String,
    val role: String = "citizen",
    val hasPassword: Boolean = true,
    val linkedGoogle: Boolean = false,
    val createdAt: String = "",
) {
    val firstName: String get() = fullName.trim().split(" ").firstOrNull().orEmpty().ifBlank { fullName }
}

@Serializable
data class Tokens(
    val accessToken: String,
    val accessTokenExpiresAt: String,
    val refreshToken: String,
    val refreshTokenExpiresAt: String,
)

@Serializable
data class AuthSession(val user: User, val tokens: Tokens)

@Serializable
data class RegisterRequest(val fullName: String, val email: String, val phone: String?, val password: String)

@Serializable
data class LoginRequest(val identifier: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class GoogleRequest(val idToken: String)

@Serializable
data class ForgotRequest(val email: String)

@Serializable
data class ResetRequest(val email: String, val code: String, val newPassword: String)

@Serializable
data class ChangePasswordRequest(val currentPassword: String?, val newPassword: String)

@Serializable
data class UpdateProfileRequest(val fullName: String? = null, val phone: String? = null)

@Serializable
data class MessageResponse(val message: String)

// ---------- Meta ----------

@Serializable
data class ServiceArea(val south: Double, val west: Double, val north: Double, val east: Double) {
    fun contains(lat: Double, lng: Double) = lat in south..north && lng in west..east
}

@Serializable
data class DatasetInfo(
    val mode: String,
    val notice: String? = null,
    val routes: Int = 0,
    val verified: Int = 0,
    val pending: Int = 0,
    val demo: Int = 0,
    val lastImportAt: String? = null,
    val lastImportSource: String? = null,
)

@Serializable
data class Features(val geocoding: Boolean = false, val googleLogin: Boolean = false, val pushNotifications: Boolean = false)

@Serializable
data class ReportTypeInfo(val key: String, val label: String, val description: String, val validityMinutes: Int)

@Serializable
data class AppMeta(
    val city: String,
    val serviceArea: ServiceArea,
    val dataset: DatasetInfo,
    val features: Features,
    val reportTypes: List<ReportTypeInfo>,
)

// ---------- Rutas y paraderos ----------

@Serializable
data class RouteStatus(
    val level: String,
    val label: String,
    val activeReports: Int = 0,
    val lastSightingMinutesAgo: Int? = null,
)

@Serializable
data class RouteSummary(
    val id: String,
    val code: String,
    val name: String,
    val color: String? = null,
    val operatorName: String? = null,
    val from: String? = null,
    val to: String? = null,
    val verification: String,
    val frequencyLabel: String,
    val status: RouteStatus? = null,
    val isFavorite: Boolean = false,
)

@Serializable
data class StopRef(val id: String, val name: String, val lat: Double, val lng: Double, val landmark: String? = null)

@Serializable
data class RouteDirection(val direction: Int, val headsign: String, val stops: List<StopRef>, val shape: List<List<Double>> = emptyList())

@Serializable
data class RouteDetail(
    val id: String,
    val code: String,
    val name: String,
    val color: String? = null,
    val operatorName: String? = null,
    val from: String? = null,
    val to: String? = null,
    val verification: String,
    val frequencyLabel: String,
    val status: RouteStatus? = null,
    val isFavorite: Boolean = false,
    val description: String? = null,
    val fareCop: Int? = null,
    val firstDeparture: String? = null,
    val lastDeparture: String? = null,
    val headwayPeakMin: Int? = null,
    val headwayOffpeakMin: Int? = null,
    val serviceDays: String? = null,
    val dataSource: String = "",
    val verifiedAt: String? = null,
    val updatedAt: String = "",
    val directions: List<RouteDirection> = emptyList(),
    val reports: List<Report> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
)

@Serializable
data class StopItem(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val landmark: String? = null,
    val verification: String = "pendiente",
    val distanceMeters: Int? = null,
    val routes: List<String> = emptyList(),
)

// ---------- Planificador ----------

@Serializable
data class LatLngDto(val lat: Double, val lng: Double)

@Serializable
data class PlaceDto(val lat: Double, val lng: Double, val label: String? = null)

@Serializable
data class PlanRequest(val origin: PlaceDto, val destination: PlaceDto, val maxWalkMeters: Int)

@Serializable
data class LegRoute(
    val id: String,
    val code: String,
    val name: String,
    val color: String? = null,
    val verification: String,
    val operatorName: String? = null,
)

@Serializable
data class StopPoint(val lat: Double, val lng: Double, val label: String, val stopId: String? = null, val stopName: String? = null)

/** Tramo del viaje: `kind` = "walk" o "bus"; los campos opcionales dependen del tipo. */
@Serializable
data class PlanLeg(
    val kind: String,
    val from: LatLngDto? = null,
    val to: LatLngDto? = null,
    val fromLabel: String? = null,
    val toLabel: String? = null,
    val meters: Int? = null,
    val minutes: Int? = null,
    val route: LegRoute? = null,
    val direction: Int? = null,
    val headsign: String? = null,
    val board: StopPoint? = null,
    val alight: StopPoint? = null,
    val rideMeters: Int? = null,
    val rideMinutes: Int? = null,
    val waitMinutes: Int? = null,
    val frequencyLabel: String? = null,
    val stopsBetween: Int? = null,
    val outsideServiceHours: Boolean = false,
    val serviceHoursLabel: String? = null,
    val path: List<LatLngDto> = emptyList(),
) {
    val isBus: Boolean get() = kind == "bus"
}

@Serializable
data class PlanOption(
    val id: String,
    val type: String,
    val totalMinutes: Int,
    val walkMeters: Int,
    val transfers: Int,
    val legs: List<PlanLeg>,
    val notes: List<String> = emptyList(),
    val verification: String,
)

@Serializable
data class WalkingOnly(val meters: Int, val minutes: Int)

@Serializable
data class PlanResult(
    val options: List<PlanOption>,
    val walkingOnly: WalkingOnly? = null,
    val searchedRadiusMeters: Int,
    val message: String? = null,
    val disclaimer: String,
)

// ---------- Reportes ----------

@Serializable
data class ReportRoute(val id: String, val code: String, val name: String, val color: String? = null)

@Serializable
data class ReportStop(val id: String, val name: String? = null)

@Serializable
data class Reliability(val level: String, val label: String, val confirmations: Int, val disputes: Int)

@Serializable
data class Report(
    val id: String,
    val route: ReportRoute,
    val type: String,
    val typeLabel: String,
    val direction: Int? = null,
    val description: String? = null,
    val location: LatLngDto? = null,
    val stop: ReportStop? = null,
    val createdAt: String,
    val expiresAt: String,
    val minutesAgo: Int,
    val isActive: Boolean,
    val source: String = "comunidad",
    val isOfficial: Boolean = false,
    val author: String,
    val isMine: Boolean = false,
    val myVote: String? = null,
    val reliability: Reliability,
)

@Serializable
data class CreateReportRequest(
    val routeId: String,
    val type: String,
    val direction: Int? = null,
    val description: String? = null,
    val stopId: String? = null,
    val location: LatLngDto? = null,
)

@Serializable
data class CreateReportMeta(val merged: Boolean, val message: String)

@Serializable
data class CreateReportResponse(val data: Report, val meta: CreateReportMeta)

@Serializable
data class VoteRequest(val vote: String)

@Serializable
data class FlagRequest(val reason: String)

@Serializable
data class Announcement(
    val id: String,
    val title: String,
    val body: String,
    val routeId: String? = null,
    val routeCode: String? = null,
    val source: String,
    val isOfficial: Boolean = true,
    val startsAt: String,
    val endsAt: String? = null,
    val createdAt: String = "",
)

// ---------- Favoritos ----------

@Serializable
data class FavoriteRoute(
    val routeId: String,
    val code: String,
    val name: String,
    val color: String? = null,
    val verification: String,
    val notify: Boolean,
    val savedAt: String,
)

@Serializable
data class NamedPlace(val label: String, val lat: Double, val lng: Double)

@Serializable
data class SavedTrip(val id: String, val name: String, val origin: NamedPlace, val destination: NamedPlace, val createdAt: String)

@Serializable
data class SaveTripRequest(val name: String, val origin: NamedPlace, val destination: NamedPlace)

@Serializable
data class NotifyRequest(val notify: Boolean)

// ---------- Notificaciones ----------

@Serializable
data class AppNotification(
    val id: String,
    val kind: String,
    val title: String,
    val body: String,
    val routeId: String? = null,
    val reportId: String? = null,
    val announcementId: String? = null,
    val createdAt: String,
    val read: Boolean,
)

@Serializable
data class UnreadCount(val unread: Int)

@Serializable
data class NotificationPrefs(
    val favoriteReports: Boolean = true,
    val serviceAlerts: Boolean = true,
    val quietStart: String? = null,
    val quietEnd: String? = null,
)

// ---------- Geocodificación ----------

@Serializable
data class GeoPlace(val label: String, val secondary: String? = null, val lat: Double, val lng: Double, val source: String = "osm")
