package com.syncapse.jive.webdav.methods

import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTimeZone
import java.io.Writer
import java.util.HashMap
import java.util.Locale
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

import net.sf.webdav.IMethodExecutor
import net.sf.webdav.ITransaction
import net.sf.webdav.StoredObject
import net.sf.webdav.WebdavStatus
import net.sf.webdav.exceptions.LockFailedException
import net.sf.webdav.fromcatalina.URLEncoder
import net.sf.webdav.fromcatalina.XMLWriter
import net.sf.webdav.locking.IResourceLocks
import net.sf.webdav.locking.LockedObject
import com.syncapse.jive.webdav.{WebdavStatus, StoredObject, MethodExecutor}
import com.syncapse.jive.webdav.fromcatalina.{XMLWriter, URLEncoder}

object AbstractMethod {
  /**
   * Array containing the safe characters set.
   */
  protected lazy val URL_ENCODER = new URLEncoder
  URL_ENCODER.addSafeCharacter('-')
  URL_ENCODER.addSafeCharacter('_')
  URL_ENCODER.addSafeCharacter('.')
  URL_ENCODER.addSafeCharacter('*')
  URL_ENCODER.addSafeCharacter('/')

  /**
   * Default depth is infite.
   */
  protected val INFINITY = 3

  /**
   * Simple date format for the creation date ISO 8601 representation
   * (partial).
   */
  protected lazy val CREATION_DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(DateTimeZone.UTC)

  /**
   * Simple date format for the last modified date. (RFC 822 updated by RFC
   * 1123)
   */
  protected lazy val LAST_MODIFIED_DATE_FORMAT = DateTimeFormat.forStyle("EEE, dd MMM yyyy HH:mm:ss z").withLocale(Locale.US).withZone(DateTimeZone.UTC)


  /**
   * size of the io-buffer
   */
  protected val BUF_SIZE = 65536

  /**
   * Default lock timeout value.
   */
  protected val DEFAULT_TIMEOUT = 3600

  /**
   * Maximum lock timeout.
   */
  protected val MAX_TIMEOUT = 604800

  /**
   * Boolean value to temporary lock resources (for method locks)
   */
  protected val TEMPORARY = true

  /**
   * Timeout for temporary locks
   */
  protected val TEMP_TIMEOUT = 10

  private val NULL_RESOURCE_METHODS_ALLOWED = "OPTIONS, MKCOL, PUT, PROPFIND, LOCK, UNLOCK"

  private val RESOURCE_METHODS_ALLOWED = "OPTIONS, GET, HEAD, POST, DELETE, TRACE"
  +", PROPPATCH, COPY, MOVE, LOCK, UNLOCK, PROPFIND"

  private val FOLDER_METHOD_ALLOWED = ", PUT"

  private val LESS_ALLOWED_METHODS = "OPTIONS, MKCOL, PUT"

  /**
   * Determines the methods normally allowed for the resource.
   *
   * @param so
   *      StoredObject representing the resource
   * @return all allowed methods, separated by commas
   */
  protected def determineMethodsAllowed(so: StoredObject): String = {

    try {
      if (so != null) {
        if (so.isNullResource()) {

          return NULL_RESOURCE_METHODS_ALLOWED

        } else if (so.isFolder()) {
          return RESOURCE_METHODS_ALLOWED + FOLDER_METHOD_ALLOWED
        }
        // else resource
        return RESOURCE_METHODS_ALLOWED
      }
    } catch {
      case e: Exception =>
      // we do nothing, just return less allowed methods
    }

    LESS_ALLOWED_METHODS
  }


}

trait AbstractMethod extends MethodExecutor {

  /**
   * Return the relative path associated with this servlet.
   *
   * @param request
   *      The servlet request we are processing
   */
  protected def getRelativePath(request: HttpServletRequest): String = {

    // Are we being processed by a RequestDispatcher.include()?
    if (request.getAttribute("javax.servlet.include.request_uri") != null) {
      var result = request.getAttribute("javax.servlet.include.path_info")
      // if (result == null)
      // result = (String) request
      // .getAttribute("javax.servlet.include.servlet_path")
      if ((result == null) || (result.equals("")))
        result = "/"
      result
    }
    else {

      // No, extract the desired path directly from the request
      String result = request.getPathInfo()
      // if (result == null) {
      // result = request.getServletPath()
      // }
      if ((result == null) || (result.equals(""))) {
        result = "/"
      }
      result
    }
  }

  /**
   * creates the parent path from the given path by removing the last '/' and
   * everything after that
   *
   * @param path
   *      the path
   * @return parent path
   */
  protected def getParentPath(path: String): String = {
    val slash = path.lastIndexOf('/')
    if (slash != -1) {
      path.substring(0, slash)
    }
    else {
      null
    }
  }

  /**
   * removes a / at the end of the path string, if present
   *
   * @param path
   *      the path
   * @return the path without trailing /
   */
  protected def getCleanPath(path: String): String = {

    if (path.endsWith("/") && path.length() > 1) {
      path.substring(0, path.length() - 1)
    }
    else {
      null
    }
  }

  /**
   * Return JAXP document builder instance.
   */
  protected def getDocumentBuilder(): DocumentBuilder = {
    try {
      val documentBuilderFactory = DocumentBuilderFactory.newInstance()
      documentBuilderFactory.setNamespaceAware(true)
      val documentBuilder = documentBuilderFactory.newDocumentBuilder()
      documentBuilder
    } catch {
      case e: ParserConfigurationException =>
        throw new ServletException("jaxp failed")
    }
  }

  /**
   * reads the depth header from the request and returns it as a int
   *
   * @param req
   * @return the depth from the depth header
   */
  protected def getDepth(req: HttpServletRequest): Int = {
    var depth = INFINITY
    val depthStr = req.getHeader("Depth")

    if (depthStr != null) {
      if (depthStr.equals("0")) {
        depth = 0
      } else if (depthStr.equals("1")) {
        depth = 1
      }
    }
    depth
  }

  /**
   * URL rewriter.
   *
   * @param path
   *      Path which has to be rewiten
   * @return the rewritten path
   */
  protected def rewriteUrl(path: String): String = URL_ENCODER.encode(path)

  /**
   * Get the ETag associated with a file.
   *
   * @param StoredObject
   *      StoredObject to get resourceLength, lastModified and a hashCode of
   *      StoredObject
   * @return the ETag
   */
  protected def getETag(so: StoredObject): String = {

    var resourceLength = ""
    var lastModified = ""

    if (so != null && so.isResource) {
      resourceLength = so.getResourceLength().toLong.toString
      lastModified = so.getLastModified.getTime.toString
    }

    "W/\"" + resourceLength + "-" + lastModified + "\""
  }

  protected def getLockIdFromIfHeader(req: HttpServletRequest): Array[String] = {
    var ids = new Array[String](2)
    var id = req.getHeader("If")

    if (id != null && !id.equals("")) {
      if (id.indexOf(">)") == id.lastIndexOf(">)")) {
        id = id.substring(id.indexOf("(<"), id.indexOf(">)"))

        if (id.indexOf("locktoken:") != -1) {
          id = id.substring(id.indexOf(':') + 1)
        }
        ids.apply(0) = id
      } else {
        var firstId = id.substring(id.indexOf("(<"), id
                .indexOf(">)"))
        if (firstId.indexOf("locktoken:") != -1) {
          firstId = firstId.substring(firstId.indexOf(':') + 1)
        }
        ids.apply(0) = firstId

        var secondId = id.substring(id.lastIndexOf("(<"), id
                .lastIndexOf(">)"))
        if (secondId.indexOf("locktoken:") != -1) {
          secondId = secondId.substring(secondId.indexOf(':') + 1)
        }
        ids.apply(0) = secondId
      }

    } else {
      ids = null
    }
    ids
  }

  protected def getLockIdFromLockTokenHeader(req: HttpServletRequest): String = {
    var id = req.getHeader("Lock-Token")

    if (id != null) {
      id = id.substring(id.indexOf(":") + 1, id.indexOf(">"))
    }

    id
  }

  /**
   * Send a multistatus element containing a complete error report to the
   * client.
   *
   * @param req
   *      Servlet request
   * @param resp
   *      Servlet response
   * @param errorList
   *      List of error to be displayed
   */
  protected def sendReport(req: HttpServletRequest, resp: HttpServletResponse,
                           errorList: Map[String, Integer]) = {

    resp.setStatus(WebdavStatus.SC_MULTI_STATUS)

    val absoluteUri = req.getRequestURI()
    // String relativePath = getRelativePath(req)

    val namespaces = new HashMap[String, String]()
    namespaces.put("DAV:", "D")

    val generatedXML = new XMLWriter(namespaces)
    generatedXML.writeXMLHeader()

    generatedXML.writeElement("DAV::multistatus", XMLWriter.OPENING)

    val pathList = errorList.keys()
    while (pathList.hasMoreElements()) {

      val errorPath = pathList.nextElement
      val errorCode = errorList.get(errorPath).intValue()

      generatedXML.writeElement("DAV::response", XMLWriter.OPENING)

      generatedXML.writeElement("DAV::href", XMLWriter.OPENING)
      String toAppend = null
      if (absoluteUri.endsWith(errorPath)) {
        toAppend = absoluteUri

      } else if (absoluteUri.contains(errorPath)) {

        int endIndex = absoluteUri.indexOf(errorPath)
        +errorPath.length()
        toAppend = absoluteUri.substring(0, endIndex)
      }
      if (!toAppend.startsWith("/") && !toAppend.startsWith("http:"))
        toAppend = "/" + toAppend
      generatedXML.writeText(errorPath)
      generatedXML.writeElement("DAV::href", XMLWriter.CLOSING)
      generatedXML.writeElement("DAV::status", XMLWriter.OPENING)
      generatedXML.writeText("HTTP/1.1 " + errorCode + " "
              + WebdavStatus.getStatusText(errorCode))
      generatedXML.writeElement("DAV::status", XMLWriter.CLOSING)

      generatedXML.writeElement("DAV::response", XMLWriter.CLOSING)

    }

    generatedXML.writeElement("DAV::multistatus", XMLWriter.CLOSING)

    Writer writer = resp.getWriter()
    writer.write(generatedXML.toString())
    writer.close()

  }

}