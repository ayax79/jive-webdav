package com.syncapse.jive.milton

import java.lang.String
import com.syncapse.jive.Loggable
import resource.{DocumentResource, CommunityResource}
import com.jivesoftware.community._
import com.syncapse.jive.auth.JiveAuthenticationProvidable
import com.bradmcevoy.http.{SecurityManager, ResourceFactory}

class JiveResourceFactory(private val jc: JiveContext,
                          private val sm: SecurityManager,
                          val contextPath: String)
  extends ResourceFactory with Loggable with JiveAuthenticationProvidable {

  def getResource(host: String, path: String) =  stripContext(path) match {
    case "" => new CommunityResource(rootCommunity, jc, sm)
    case url => findObjectFromUriTokens(tokens(url), rootCommunity) match {
      case Some(c: Community) => new CommunityResource(c, jc, sm)
      case Some(d: Document) => new DocumentResource(d, jc.getDocumentManager, sm)
      case Some(_) => throw new IllegalArgumentException("could not determine resource")
      case None => null
    }
  }

  protected def tokens(uri: String) =
    uri.split("/").toList.filter {
      case "" => false;
      case _ => true
    }

  protected def stripContext(url: String): String =
    if (contextPath != null && contextPath.length() > 0) url.replaceFirst('/' + contextPath, "");
    else if (url == "/") ""
    else url


  protected def rootCommunity = jc.getCommunityManager.getRootCommunity


  /**
   * Method takes a uri tokens, and tries to find a matching object.
   *
   * This method will be called recursively until it find the correct match.
   * A standard initial call to it may look something like this:
   * findObjectFromUriTokens(tokens(uri), rootCommunity)
   *
   * Which would mean start from the rootCommunity and use the current uri portion tokenized.
   *
   * Note that this method expects that you stripped off the /community or /spaces
   *
   * Uri Tokens:
   * If the uri is something like /one/two/three then the tokens will be List("one", "two", "three").
   *
   * @param tokens Tokens matching the uri
   * @param tokens The jive object at the current context.
   */
  protected[milton] def findObjectFromUriTokens(tokens: List[String], j: Document): Option[JiveObject] = {Some(j)}
  protected[milton] def findObjectFromUriTokens(tokens: List[String], c: Community): Option[JiveObject] = tokens match {
    case Nil => None // couldn't find a match
    case "" :: Nil => None // handle the odd case where there may be no entry
    case "" :: tail => findObjectFromUriTokens(tail, c) // handle the odd case where there may be no entry
    case head :: tail =>
      childCommunities(c).find(c => c.getName == head) match {
        case s@Some(cc) if tail == Nil => s // return the community we are out of tokens.
        case Some(cc) => findObjectFromUriTokens(tail, cc)
        case None =>
        // see if it a document instead
          childDocuments(c).find(d => d.getBinaryBody.getName == head) match {
            case None => None
            case Some(d) => findObjectFromUriTokens(tail, d)
          }
      }
  }


  protected val childCommunities = WebdavUtil.childCommunities(_: Community, jc.getCommunityManager)
  protected val childDocuments = WebdavUtil.childDocuments(_: Community, jc.getDocumentManager)

}