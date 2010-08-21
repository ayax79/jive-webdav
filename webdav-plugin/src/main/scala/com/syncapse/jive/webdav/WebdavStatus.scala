package com.syncapse.jive.webdav

import java.util.Hashtable;

import javax.servlet.http.HttpServletResponse;

/**
 * Wraps the HttpServletResponse class to abstract the specific protocol used.
 * To support other protocols we would only need to modify this class and the
 * WebDavRetCode classes.
 *
 * @author Marc Eaddy
 * @version 1.0, 16 Nov 1997
 */
object WebdavStatus {

    // ----------------------------------------------------- Instance Variables

    /**
     * This Hashtable contains the mapping of HTTP and WebDAV status codes to
     * descriptive text. This is a static variable.
     */
//    private static Hashtable<Integer, String> _mapStatusCodes = new Hashtable<Integer, String>();

    // ------------------------------------------------------ HTTP Status Codes

    /**
     * Status code (200) indicating the request succeeded normally.
     */
    val SC_OK = HttpServletResponse.SC_OK;

    /**
     * Status code (201) indicating the request succeeded and created a new
     * resource on the server.
     */
    val SC_CREATED = HttpServletResponse.SC_CREATED;

    /**
     * Status code (202) indicating that a request was accepted for processing,
     * but was not completed.
     */
    val SC_ACCEPTED = HttpServletResponse.SC_ACCEPTED;

    /**
     * Status code (204) indicating that the request succeeded but that there
     * was no new information to return.
     */
    val SC_NO_CONTENT = HttpServletResponse.SC_NO_CONTENT;

    /**
     * Status code (301) indicating that the resource has permanently moved to a
     * new location, and that future references should use a new URI with their
     * requests.
     */
    val SC_MOVED_PERMANENTLY = HttpServletResponse.SC_MOVED_PERMANENTLY;

    /**
     * Status code (302) indicating that the resource has temporarily moved to
     * another location, but that future references should still use the
     * original URI to access the resource.
     */
    val SC_MOVED_TEMPORARILY = HttpServletResponse.SC_MOVED_TEMPORARILY;

    /**
     * Status code (304) indicating that a conditional GET operation found that
     * the resource was available and not modified.
     */
    val SC_NOT_MODIFIED = HttpServletResponse.SC_NOT_MODIFIED;

    /**
     * Status code (400) indicating the request sent by the client was
     * syntactically incorrect.
     */
    val SC_BAD_REQUEST = HttpServletResponse.SC_BAD_REQUEST;

    /**
     * Status code (401) indicating that the request requires HTTP
     * authentication.
     */
    val SC_UNAUTHORIZED = HttpServletResponse.SC_UNAUTHORIZED;

    /**
     * Status code (403) indicating the server understood the request but
     * refused to fulfill it.
     */
    val SC_FORBIDDEN = HttpServletResponse.SC_FORBIDDEN;

    /**
     * Status code (404) indicating that the requested resource is not
     * available.
     */
    val SC_NOT_FOUND = HttpServletResponse.SC_NOT_FOUND;

    /**
     * Status code (500) indicating an error inside the HTTP service which
     * prevented it from fulfilling the request.
     */
    val SC_INTERNAL_SERVER_ERROR = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

    /**
     * Status code (501) indicating the HTTP service does not support the
     * functionality needed to fulfill the request.
     */
    val SC_NOT_IMPLEMENTED = HttpServletResponse.SC_NOT_IMPLEMENTED;

    /**
     * Status code (502) indicating that the HTTP server received an invalid
     * response from a server it consulted when acting as a proxy or gateway.
     */
    val SC_BAD_GATEWAY = HttpServletResponse.SC_BAD_GATEWAY;

    /**
     * Status code (503) indicating that the HTTP service is temporarily
     * overloaded, and unable to handle the request.
     */
    val SC_SERVICE_UNAVAILABLE = HttpServletResponse.SC_SERVICE_UNAVAILABLE;

    /**
     * Status code (100) indicating the client may continue with its request.
     * This interim response is used to inform the client that the initial part
     * of the request has been received and has not yet been rejected by the
     * server.
     */
    val SC_CONTINUE = 100;

    /**
     * Status code (405) indicating the method specified is not allowed for the
     * resource.
     */
    val SC_METHOD_NOT_ALLOWED = 405;

    /**
     * Status code (409) indicating that the request could not be completed due
     * to a conflict with the current state of the resource.
     */
    val SC_CONFLICT = 409;

    /**
     * Status code (412) indicating the precondition given in one or more of the
     * request-header fields evaluated to false when it was tested on the
     * server.
     */
    val SC_PRECONDITION_FAILED = 412;

    /**
     * Status code (413) indicating the server is refusing to process a request
     * because the request entity is larger than the server is willing or able
     * to process.
     */
    val SC_REQUEST_TOO_LONG = 413;

    /**
     * Status code (415) indicating the server is refusing to service the
     * request because the entity of the request is in a format not supported by
     * the requested resource for the requested method.
     */
    val SC_UNSUPPORTED_MEDIA_TYPE = 415;

    // -------------------------------------------- Extended WebDav status code

    /**
     * Status code (207) indicating that the response requires providing status
     * for multiple independent operations.
     */
    val SC_MULTI_STATUS = 207;

    // This one colides with HTTP 1.1
    // "207 Parital Update OK"

    /**
     * Status code (418) indicating the entity body submitted with the PATCH
     * method was not understood by the resource.
     */
    val SC_UNPROCESSABLE_ENTITY = 418;

    // This one colides with HTTP 1.1
    // "418 Reauthentication Required"

    /**
     * Status code (419) indicating that the resource does not have sufficient
     * space to record the state of the resource after the execution of this
     * method.
     */
    val SC_INSUFFICIENT_SPACE_ON_RESOURCE = 419;

    // This one colides with HTTP 1.1
    // "419 Proxy Reauthentication Required"

    /**
     * Status code (420) indicating the method was not executed on a particular
     * resource within its scope because some part of the method's execution
     * failed causing the entire method to be aborted.
     */
    val SC_METHOD_FAILURE = 420;

    /**
     * Status code (423) indicating the destination resource of a method is
     * locked, and either the request did not contain a valid Lock-Info header,
     * or the Lock-Info header identifies a lock held by another principal.
     */
    val SC_LOCKED = 423;

    // ------------------------------------------------------------ Initializer


    protected val statusCodeMap =
      Map(
          // Http 1.0 Status Code
          SC_OK -> "OK"
          ,SC_CREATED -> "Created"
          ,SC_ACCEPTED -> "Accepted"
          ,SC_NO_CONTENT -> "No Content"
          ,SC_MOVED_PERMANENTLY -> "Moved Permanently"
          ,SC_MOVED_TEMPORARILY -> "Moved Temporarily"
          ,SC_NOT_MODIFIED -> "Not Modified"
          ,SC_BAD_REQUEST -> "Bad Request"
          ,SC_UNAUTHORIZED -> "Unauthorized"
          ,SC_FORBIDDEN -> "Forbidden"
          ,SC_NOT_FOUND -> "Not Found"
          ,SC_INTERNAL_SERVER_ERROR -> "Internal Server Error"
          ,SC_NOT_IMPLEMENTED -> "Not Implemented"
          ,SC_BAD_GATEWAY -> "Bad Gateway"
          ,SC_SERVICE_UNAVAILABLE -> "Service Unavailable"
          ,SC_CONTINUE -> "Continue"
          ,SC_METHOD_NOT_ALLOWED -> "Method Not Allowed"
          ,SC_CONFLICT -> "Conflict"
          ,SC_PRECONDITION_FAILED -> "Precondition Failed"
          ,SC_REQUEST_TOO_LONG -> "Request Too Long"
          ,SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type"
          // WebDav Status Codes
          ,SC_MULTI_STATUS -> "Multi-Status"
          ,SC_UNPROCESSABLE_ENTITY -> "Unprocessable Entity"
          ,SC_INSUFFICIENT_SPACE_ON_RESOURCE -> "Insufficient Space On Resource"
          ,SC_METHOD_FAILURE -> "Method Failure"
          ,SC_LOCKED -> "Locked"
      )

    /**
     * Returns the HTTP status text for the HTTP or WebDav status code specified
     * by looking it up in the static mapping. This is a static function.
     *
     * @param nHttpStatusCode
     *      [IN] HTTP or WebDAV status code
     * @return A string with a short descriptive phrase for the HTTP status code
     *  (e.g., "OK").
     */
    def getStatusText(nHttpStatusCode: String): String = statusCodeMap[nHttpStatusCode.toInt] || ""


}