package com.syncapse.jive.webdav

import java.security.Principal
import java.lang.String
import com.syncapse.jive.Loggable
import scala.collection.JavaConversions
import com.jivesoftware.community._
import net.sf.webdav.exceptions.{UnauthenticatedException, WebdavException}
import org.acegisecurity.AuthenticationException
import renderer.impl.v2.JAXPUtils
import com.jivesoftware.base.wiki.JiveHtmlElement
import org.apache.commons.io.IOUtils
import java.io.{ByteArrayOutputStream, ByteArrayInputStream, InputStream}
import socialgroup.SocialGroup
import web.MimeTypeManager
import net.sf.webdav.{StoredObject, ITransaction, IWebdavStore}

class JiveWebdavStore(contextProvider: ContextProvider, tmpStore: IWebdavStore) extends IWebdavStore with Loggable with JiveAuthenticationProvidable {
  protected def documentManager = contextProvider.jiveContext.getDocumentManager

  protected def communityManager = contextProvider.jiveContext.getCommunityManager

  protected def documentTypeManager = contextProvider.jiveContext.getDocumentTypeManager

  protected def mimeTypeManager: MimeTypeManager = contextProvider.jiveContext.getSpringBean("mimeTypeManager")

  override def getStoredObject(transaction: ITransaction, uri: String) = {
    def tmpGetStored = {
      tmpStore.getStoredObject(transaction, uri) match {
        case null => None
        case s: StoredObject => Some(s)
      }
    }

    val so = JiveWebdavUtils.matchUrl(uri) {
      case CommunityUri(s) => s match {
        case "" => Some(JiveWebdavUtils.buildStoredObject(rootCommunity)) // root community
        case _ => findObjectFromUriTokens(tokens(s), rootCommunity) match {
          case None => tmpGetStored
          case Some(x) => Some(JiveWebdavUtils.buildStoredObject(x))
        }
      }
      case SpacesUri(s) => s match {
        case "" => Some(JiveWebdavUtils.RootStoredObject)
        case _ => tmpGetStored
      }
      case RootUri => Some(JiveWebdavUtils.RootStoredObject)
      case _ => tmpGetStored
    } match {
      case Some(x) => x
      case None => null
    }
    logger.info("JiveWebStore getStoredObject: " + uri + " storedObject: " + printSo(so))
    so
  }

  override def removeObject(transaction: ITransaction, uri: String) = {
    logger.info("JiveWebStore removeObject: " + uri)
    throw new WebdavException("unsupported operation")
  }

  override def getResourceLength(transaction: ITransaction, path: String) = {
    def tmpGetResourceLength = Some(tmpStore.getResourceLength(transaction, path))

    logger.info("JiveWebStore getResourceLength: " + path)
    JiveWebdavUtils.matchUrl(path) {
      case CommunityUri(rest) => findObjectFromUriTokens(tokens(rest), rootCommunity) match {
        case Some(x) => x match {
          case d: Document => Some(JiveWebdavUtils.buildStoredObject(x).getResourceLength)
          case _ => None
        }
        // check to see if their is a tmp file for it
        case None => tmpGetResourceLength
      }
      case _ => tmpGetResourceLength
    } match {
      case Some(s) => s
      case None => 0L
    }
  }

  override def getChildrenNames(transaction: ITransaction, folderUri: String) = {


    val children = JiveWebdavUtils.matchUrl(folderUri) {
      case RootUri => Some(Array("communities", "spaces"))
      case CommunityUri(rest) => rest match {
        case "" => Some(communityNames(rootCommunity) ++ documentNames(rootCommunity)) // root community
        case _ => findObjectFromUriTokens(tokens(rest), rootCommunity) match {
          case Some(x) => x match {
            case c: Community => Some(communityNames(c) ++ documentNames(c))
            case _ => None
          }
          case _ => None
        }
      }
      case _ => None
    } match {
      case Some(x) => x
      case None => null
    }

    logger.info("JiveWebStore getChildrenNames: " + folderUri + " children: " + printList(children))
    children
  }


  override def setResourceContent(transaction: ITransaction, resourceUri: String, content: InputStream, contentType: String, characterEncoding: String) = {
    logger.info("JiveWebStore setResourceContent: " + resourceUri)
    JiveWebdavUtils.matchUrl(resourceUri) {
      case CommunityUri(rest) => rest match {
        case "" => None // we don't care about the root community
        case _ => findObjectFromUriTokens(tokens(rest), rootCommunity) match {
          case Some(x) => x match {
            case d: Document => d.isTextBody match {
              case true =>
                val out = new ByteArrayOutputStream
                IOUtils.copy(content, out)
                d.setBody(JAXPUtils.createDocument(JiveHtmlElement.Body.getTag(), new String(out.toByteArray)))
                d.save
                Some(d)
              case false =>
                val bb: BinaryBody = d.getBinaryBody
                d.setBinaryBody(bb.getName, contentType, content)
                d.save
                Some(d)
            }
            case _ => None // we only care about documents
          }
          case None => None // didn't find anything that matched the url
        }
      }
    } match {
      case Some(d) => JiveWebdavUtils.buildStoredObject(d).getResourceLength
      case None => 0L
    }
  }

  override def getResourceContent(transaction: ITransaction, resourceUri: String) = JiveWebdavUtils.matchUrl(resourceUri) {
    case CommunityUri(rest) => rest match {
      case "" => None // root community
      case _ => findObjectFromUriTokens(tokens(rest), rootCommunity) match {
        case Some(x) =>
          x match {
            case d: Document =>
              d.isTextBody match {
                case true => Some(new ByteArrayInputStream(d.getPlainBody.getBytes("utf-8")))
                case false => Some(d.getBinaryBody.getData)
              }
            case _ => None
          }
        case _ => None
      }
    }
    case _ => None
  } match {
    case Some(s) => s
    case None => null
  }


  override def createResource(transaction: ITransaction, resourceUri: String) = {
    logger.info("JiveWebStore createResource: " + resourceUri)
    JiveWebdavUtils.matchUrl(resourceUri) {
      case CommunityUri(rest) => rest match {
        case "" => None
        case _ =>
          createNewItem(tokens(rest)) {
            case (name: String, jc: JiveObject) => jc match {
              case c: Community =>
                currentUser match {
                  case Some(u) =>
                    val dt = documentTypeManager.createDocumentType(name, name) // use the name as the description
                    val doc: Document = documentManager.createDocument(u, dt, null, name, "")
                    val mimeType = mimeTypeManager.getExtensionMimeType(name)
                    doc.setBinaryBody(name, mimeType, new ByteArrayInputStream(name.getBytes("UTF-8")))
                    documentManager.addDocument(c, doc, null)
                    Some(doc)
                  case None => None // user is not authenticated
                }
              case _ => None // we are only handling documents created under a community
            }
          }
      }
      case _ => None // we are only handling community urls 
    }
  }

  override def createFolder(transaction: ITransaction, folderUri: String) = {
    logger.info("JiveWebStore createFolder: " + folderUri)
    JiveWebdavUtils.matchUrl(folderUri) {
      case CommunityUri(rest) => rest match {
        case "" => None
        case _ =>
          createNewItem(tokens(rest)) {
            case (name: String, jc: JiveObject) => jc match {
              case c: Community =>
                try {
                  Some(communityManager.createCommunity(c, name, name, name))
                }
                catch {
                  case e: AuthenticationException =>
                    logger.warn(e.getMessage, e)
                    throw new UnauthenticatedException(e.getMessage, e)
                  case ex: Exception =>
                    logger.warn(ex.getMessage, ex)
                    throw new WebdavException(ex)
                }
              case _ => None
            }
          }
      }
      case _ => None
    } match {
      case Some(s) => s // Just return the result
      case None => new WebdavException("Cannot create a folder at: " + folderUri) // Dies since we couldn't create the folder
    }
  }

  override def rollback(transaction: ITransaction) = {
    logger.info("rollback called")
  }

  override def commit(transaction: ITransaction) = {
    logger.info("commit called")
  }

  override def checkAuthentication(transaction: ITransaction) = {
    logger.info("checkAuthentication called " + transaction)
  }

  override def begin(principal: Principal) = {
    logger.info("begin called principal: " + principal)
    new ITransaction {
      def getPrincipal = principal
    }
  }

  protected def rootCommunity = communityManager.getRootCommunity

  protected def matchingCommunity(c: Community, name: String): Option[Community] = {
    logger.info("matchingCommunity c: " + c.getName + " name: " + name)
    // todo, caching or something more efficient here
    val communities: Iterable[Community] = JavaConversions.asIterable(communityManager.getCommunities(c))
    communities.find(c => c.getName == name) match {
      case Some(x) => Some(x)
      case None => None
    }
  }

  protected[webdav] def findObjectFromUriTokens(tokens: List[String], j: JiveObject): Option[JiveObject] = j match {
  // go until we find a document, even if there are more tokens (we will ignore them)
    case d: Document => Some(d)
    case c: Community =>
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
                    case Nil => Some(c) // return the community we are out of tokens.
                    case _ => findObjectFromUriTokens(tail, c)
                  }
              }
          }

      }
  }

  protected def documentFromCommunity(s: String, c: Community): Option[Document] = {
    logger.info("documentFromCommunity s: " + s + " c: " + c)
    val docs: Iterable[Document] = loadDocuments(c)
    docs.find(d => documentTitle(d) == s) match {
      case None => None
      case Some(d) => Some(d)
    }
  }

  protected def printSo(so: StoredObject) = so match {
    case null => " NULL "
    case _ => "folder: " + so.isFolder + ",creationDate" + so.getCreationDate + ",lastModified: " + so.getLastModified
  }

  protected def printList(list: Array[String]): String = list match {
    case null => null
    case _ => printList(list.toList)
  }

  protected def printList(list: List[String]): String = list match {
    case Nil => ""
    case _ => "[" + list.reduceLeft(_ + ", " + _) + "]"
  }

  protected def tokens(uri: String) = {
    val list: List[String] = uri.split("/").toList
    list.filter {
      case "" => false;
      case _ => true
    }
  }

  protected def documentTitle(d: Document): String = d.isTextBody match {
    case true => d.getSubject
    case false => d.getBinaryBody.getName
  }

  protected def loadDocuments(c: JiveContainer): List[Document] = {
    val filter = DocumentResultFilter.createDefaultFilter
    filter.setRecursive(false)
    val list: List[Document] = JavaConversions.asIterable(documentManager.getDocuments(c, filter)).toList
    // right now we only want binary documentts and it appears that for some reason there is a bug
    // in jive where recursive docs are always returned, double check.
    list.filter {
      d => !d.isTextBody && c.getID == d.getContainerID
    }
  }

  /**
   * Handles determining which part of the url is the item to be created and which part is
   * the container it should be created under. After determining it will pass information to the closure.
   */
  protected[webdav] def createNewItem(tokens: List[String])(f: (String, JiveObject) => Option[AnyRef]): Option[AnyRef] = tokens.reverse match {
  // we need to seperate the last item from the url from the rest of the list
  // the last item will be the name of the community
  // the rest of the url tokens will be the communities underneath
    case Nil => None
    case head :: tail =>
      // reverse the tail half the list back again so that we can acquire the parent community
      findObjectFromUriTokens(tail.reverse, rootCommunity) match {
        case Some(x) => x match {
          case c: Community => f(head, x)
          case sg: SocialGroup => f(head, x)
          case _ => None
        }
        case None => None
      }
  }

  protected def communityNames(community: Community) = {
    val communities: Iterable[Community] = JavaConversions.asIterable(communityManager.getCommunities(community))
    communities.map(c => c.getName).toArray[String]
  }

  protected def documentNames(jco: JiveContainer) = {
    val docs = loadDocuments(jco)
    docs.map(d => documentTitle(d)).toArray[String]
  }


}