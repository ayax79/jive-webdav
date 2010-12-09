package com.syncapse.jive.webdav

import net.sf.webdav.StoredObject
import com.jivesoftware.community._
import java.util.Date
import socialgroup.SocialGroup

/**
 * Trait used by all UriCase case classes and objects. These are meant to be used with JiveWebUtils#matchUri
 */
trait UriCase

/**
 * Used to denote that a community url has been matched.
 * The root community will just be "", so you are encouraged to match this as a seperated case.
 *
 * @param rest The rest of the url after /communities (e.g. /one)
 */
case class CommunityUri(rest: String) extends UriCase

/**
 * Used to denote that a space has been matched
 * @param rest The rest of the url after /spaces
 */
case class SpacesUri(rest: String) extends UriCase

/**
 * Used to denote that a url that should be ignored has been matched.
 */
case object IgnoredUri extends UriCase

/**
 * Used to denote that the root uri has been matched
 */
case object RootUri extends UriCase

/**
 * Used to denote that an unknown or unmatched url has occurred.
 */
case object NonMatchUri extends UriCase

object JiveWebdavUtils {
  protected lazy val EPOCH = new Date(0)

  object RootStoredObject extends StoredObject {
    setFolder(true)
    setLastModified(EPOCH)
    setCreationDate(EPOCH)
    def asStoredObject = this.asInstanceOf[StoredObject]
  }

  object EmptyResourceObject extends StoredObject {
    setNullResource(true)
    setLastModified(EPOCH)
    setCreationDate(EPOCH)
    def asStoredObject = this.asInstanceOf[StoredObject]
  }

  def buildStoredObject(jo: JiveObject): StoredObject = jo match {
    case c: Community => buildStoredObjectFromContainer(c.asInstanceOf[JiveContainer])
    case sg: SocialGroup => buildStoredObjectFromContainer(sg.asInstanceOf[JiveContainer])
    case d: Document => buildContentObject(d)
    case _ => throw new IllegalStateException("Case class not handled " + jo)
  }


  /**
   * Handles the matching the uri, and passes a result to the the closure.
   * The result will be an instance of UriCase.
   *
   * This allows for some hacks to be make in order to handle some of the cases that were a bit of a pain in the ass to
   * do otherwise.
   */
  def matchUrl[A](uri: String)(f: AnyRef => Option[A]): Option[A] =
    if (matchesIgnored(uri)) f(IgnoredUri)
    else uri match {
      case "/" => f(RootUri)
      case "/communities" => f(CommunityUri(""))
      case "/spaces" => f(SpacesUri(""))
      case _ =>
        val s: Seq[Char] = uri
        s match {
        // i used to do this with regexp matching, but these seems to do a better job of just matching the first chunk
        // while matching anything after it
          case Seq('/', 'c', 'o', 'm', 'm', 'u', 'n', 'i', 't', 'i', 'e', 's', rest@_*) => f(CommunityUri(rest.toString))
          case Seq(_*) => f(NonMatchUri)
        }
    }

  protected def matchesIgnored(uri: String): Boolean =
    if (uri.contains("/.")) true
    else if (uri.contains(".tmp") || uri.contains(".TMP")) true
    else false

  protected def buildStoredObjectFromContainer(jc: JiveContainer): StoredObject = {
    val so = new StoredObject
    so.setCreationDate(jc.getCreationDate)
    so.setFolder(true)
    so.setLastModified(jc.getModificationDate)
    so.setResourceLength(0L)
    so
  }

  protected def buildContentObject(d: Document): StoredObject = {
    val so = new StoredObject
    so.setCreationDate(d.getCreationDate)
    so.setLastModified(d.getModificationDate)
    val binaryBody = d.getBinaryBody
    val size = if (binaryBody != null) {
      binaryBody.getSize
    }
    else {
      d.getPlainBody.getBytes.length.toLong
    }
    so.setResourceLength(size)
    so
  }

}