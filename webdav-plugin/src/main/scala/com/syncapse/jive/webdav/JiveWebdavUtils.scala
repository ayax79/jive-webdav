package com.syncapse.jive.webdav

import net.sf.webdav.StoredObject
import com.jivesoftware.community._
import java.util.Date

trait JiveCaseClass
case class CommunityCase(jiveObject: Community) extends JiveCaseClass
case class SocialGroupCase(jiveObject: SocialGroupCase) extends JiveCaseClass
case class DocumentCase(jiveObject: Document) extends JiveCaseClass

trait UriCase
case class CommunityUri(s: String) extends UriCase
case class SpacesUri(s: String) extends UriCase
case object IgnoredUri extends UriCase
case object RootUri extends UriCase
case object NonMatchUri extends UriCase

object JiveWebdavUtils {
  lazy val CommunitiesRE = """\/communities\/([^\s]*)$""".r
  lazy val SpacesRE = """\/spaces\/([^\s])*$""".r
  lazy val IgnoredRE = """([^\.]*\/\.[\w\_\s\.]*)""".r // hacks for some files on mac os x we should ignore.

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

  def buildStoredObject(jo: JiveCaseClass): StoredObject = jo match {
    case CommunityCase(c) => buildStoredObjectFromContainer(c.asInstanceOf[JiveContainer])
    case SocialGroupCase(c) => buildStoredObjectFromContainer(c.asInstanceOf[JiveContainer])
    case DocumentCase(d) => buildContentObject(d)
    case _ => throw new IllegalStateException("Case class not handled " + jo)
  }


  def matchUrl[A](uri: String)(f: AnyRef => Option[A]): Option[A] = {
    uri match {
      case "/" => f(RootUri)
      case "/communities" => f(CommunityUri(""))
      case "/spaces" => f(SpacesUri(""))
      case IgnoredRE(x) => f(IgnoredUri)
      case CommunitiesRE(rest) => f(CommunityUri(rest))
      case _ => f(NonMatchUri)
    }
  }

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