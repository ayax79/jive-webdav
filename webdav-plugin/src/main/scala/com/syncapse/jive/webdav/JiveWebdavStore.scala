package com.syncapse.jive.webdav

import java.security.Principal
import java.lang.String
import com.syncapse.jive.Loggable
import net.sf.webdav.{StoredObject, ITransaction, IWebdavStore}
import scala.collection.JavaConversions
import com.jivesoftware.community._
import java.net.URLEncoder
import java.io.{ByteArrayInputStream, InputStream}
import net.sf.webdav.exceptions.WebdavException
import java.util.{List => JList, Date}

class JiveWebdavStore(jiveContext: JiveContext) extends IWebdavStore with Loggable {
  lazy val CommunitiesRE = """\/communities\/([^\s]*)$""".r
  lazy val SpacesRE = """\/spaces\/([^\s])*$""".r
  lazy val IgnoredRE = """(\.DS\_Store|\.\_)""".r // hacks for some files on mac os x we should ignore.

  lazy val documentManager = jiveContext.getDocumentManager
  lazy val communityManager = jiveContext.getCommunityManager

  def getStoredObject(transaction: ITransaction, uri: String) = {
    val so = uri match {
      case "/" => RootStoredObject.asInstanceOf[StoredObject]
      case "/communities" => JiveWebdavUtils.buildStoredObject(rootCommunity)
      case "/spaces" => RootStoredObject.asInstanceOf[StoredObject]
      case IgnoredRE => null
      case CommunitiesRE(rest) =>
        findObjectFromUriTokens(tokens(rest), rootCommunity) match {
          case None => null
          case Some(x) => JiveWebdavUtils.buildStoredObject(x)
        }
      case _ => null // just put something so we don't get an NPE
    }
    logger.info("JiveWebStore getStoredObject: " + uri + " storedObject: " + printSo(so))
    so
  }

  def removeObject(transaction: ITransaction, uri: String) = {
    logger.info("JiveWebStore removeObject: " + uri)
    throw new WebdavException("unsupported operation")
  }

  def getResourceLength(transaction: ITransaction, path: String) = {
    logger.info("JiveWebStore getResourceLength: " + path)
    path match {
      case "/" | "/communities" | "/spaces" | IgnoredRE => 0L
      case CommunitiesRE(rest) => findObjectFromUriTokens(tokens(rest), rootCommunity) match {
        case Some(x) => x match {
          case CommunityCase(c) => 0L
          case DocumentCase(d) => JiveWebdavUtils.buildStoredObject(x).getResourceLength
        }
        case None => 0L
      }
      case _ => 0L
    }
  }

  def getChildrenNames(transaction: ITransaction, folderUri: String) = {
    def communityNames(community: Community) = {
      val communities: Iterable[Community] = JavaConversions.asIterable(communityManager.getCommunities(community))
      communities.map(c => c.getName).toArray[String]
    }

    def documentNames(jco: JiveContainer) = {
      val docs = loadDocuments(jco)
      docs.map(d => documentTitle(d)).toArray[String]
    }

    val children = folderUri match {
      case "/" => Array[String]("communities", "spaces")
      case IgnoredRE => null
      case "/communities" =>
        communityNames(rootCommunity.jiveObject) ++ documentNames(rootCommunity.jiveObject)
      case CommunitiesRE(rest) =>
        findObjectFromUriTokens(tokens(rest), rootCommunity) match {
          case Some(x) => x match {
            case CommunityCase(c) => communityNames(c) ++ documentNames(c)
            case _ =>
              logger.info("x is not a community: returning empty array: x : " + x)
              Array[String]() // either a document or None was returned
          }
          case _ =>
            logger.info("did not find a community, found None")
            null
        }
      case _ =>
        logger.info("returning empty array for folderUri: " + folderUri)
        null
    }
    logger.info("JiveWebStore getChildrenNames: " + folderUri + " children: " + printList(children))
    children
  }


  def setResourceContent(transaction: ITransaction, resourceUri: String, content: InputStream, contentType: String, characterEncoding: String) = {
    logger.info("JiveWebStore setResourceContent: " + resourceUri)
    throw new WebdavException("unsupported operation")
  }

  def getResourceContent(transaction: ITransaction, resourceUri: String) = resourceUri match {
    case "/" | IgnoredRE | "/communities" | "/spaces" => null
    case CommunitiesRE(rest) =>
      findObjectFromUriTokens(tokens(rest), rootCommunity) match {
        case Some(x) =>
          x match {
            case DocumentCase(d) =>
              d.isTextBody match {
                case true =>
                  new ByteArrayInputStream(d.getPlainBody.getBytes("utf-8"))
                case false =>
                  d.getBinaryBody.getData
              }
            case _ => null
          }
        case _ => null
      }
    case _ => null
  }

  def createResource(transaction: ITransaction, resourceUri: String) = {
    logger.info("JiveWebStore createResource: " + resourceUri)
    throw new WebdavException("unsupported operation")
  }

  def createFolder(transaction: ITransaction, folderUri: String) = {
    logger.info("JiveWebStore createFolder: " + folderUri)
    throw new WebdavException("unsupported operation")
  }

  def rollback(transaction: ITransaction) = {
    logger.info("rollback called")
  }

  def commit(transaction: ITransaction) = {
    logger.info("commit called")
  }

  def checkAuthentication(transaction: ITransaction) = {
    logger.info("checkAuthentication called")
  }

  def begin(principal: Principal) = {
    logger.info("begin called principal: " + principal)
    new ITransaction {
      def getPrincipal = principal
    }
  }

  def rootCommunity = CommunityCase(communityManager.getRootCommunity)

  def matchingCommunity(c: Community, name: String): Option[CommunityCase] = {
    logger.info("matchingCommunity c: " + c.getName + " name: " + name)
    // todo, caching or something more efficient here
    val communities: Iterable[Community] = JavaConversions.asIterable(communityManager.getCommunities(c))
    communities.find(c => c.getName == name) match {
      case Some(x) => Some(CommunityCase(x))
      case None => None
    }
  }

  def findObjectFromUriTokens(tokens: List[String], j: JiveCaseClass): Option[JiveCaseClass] = j match {
  // go until we find a document, even if there are more tokens (we will ignore them)
    case DocumentCase(d) => Some(DocumentCase(d))
    case CommunityCase(c) =>
      tokens match {
        case Nil => None // couldn't find a match
        case head :: tail =>
          head match {
          // handle the odd case where there may be no entry
            case "" =>
              tail match {
                case Nil => None
                case _ => findObjectFromUriTokens(tail, j)
              }
            case _ =>
              matchingCommunity(c, head) match {
                case None =>
                  // see if it a document instead
                  documentFromCommunity(head, c) match {
                    case None => None
                    case Some(d) => findObjectFromUriTokens(tail, d)
                  }
                case Some(c) =>
                  tail match {
                    case Nil => Option(c) // return the community we are out of tokens. 
                    case _ => findObjectFromUriTokens(tail, c)
                  }
              }
          }

      }
  }

  def documentFromCommunity(s: String, c: Community): Option[DocumentCase] = {
    logger.info("documentFromCommunity s: " + s + " c: " + c)
    val docs: Iterable[Document] = loadDocuments(c)
    docs.find(d => documentTitle(d) == s) match {
      case None => None
      case Some(d) => Some(DocumentCase(d))
    }
  }

  object RootStoredObject extends StoredObject {
    setFolder(true)
    setLastModified(new Date(0))
    setCreationDate(new Date(0))
  }

  def printSo(so: StoredObject) = if (so != null) {
    "folder: " + so.isFolder + ",creationDate" + so.getCreationDate + ",lastModified: " + so.getLastModified
  } else {" NULL "}


  def printList(list: Array[String]): String = list match {
    case null => null
    case _ => printList(list.toList)
  }

  def printList(list: List[String]): String = list match {
    case Nil => ""
    case _ =>
      "[" + list.reduceLeft(_ + ", " + _) + "]"
  }

  def tokens(uri: String) = {
    val list: List[String] = uri.split("/").toList
    list.filter {case "" => false; case _ => true}
  }

  def documentTitle(d: Document) : String = {
    val content = d.isTextBody match {
      case true => d.getSubject
      case false => d.getBinaryBody.getName
    }
    URLEncoder.encode(content, "utf-8")
  }

  def loadDocuments(c: JiveContainer): List[Document] = {
    val filter = DocumentResultFilter.createDefaultFilter
    filter.setRecursive(false)
    val list: List[Document] = JavaConversions.asIterable(documentManager.getDocuments(c, filter)).toList
    // right now we only want binary documentts and it appears that for some reason there is a bug
    // in jive where recursive docs are always returned, double check.
    list.filter { d => !d.isTextBody && c.getID == d.getContainerID } 
  }

}