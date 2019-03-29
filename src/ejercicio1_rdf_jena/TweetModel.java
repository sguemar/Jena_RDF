package ejercicio1_rdf_jena;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import javax.swing.JOptionPane;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.DC_11;
import org.apache.jena.vocabulary.ORG;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RSS;
import org.apache.jena.vocabulary.VCARD;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class TweetModel {
    private final Model model;
    private final String tweetURI = "http://si2twitter.com/tweet/";
    private final String authorURI = "http://si2twitter.com/author/";
    private final String languageURI = "http://si2twitter.com/language/";
    private final String hashtagURI = "http://si2twitter.com/hashtag/";
    private final String topicURI = "http://si2twitter.com/topic/";
    private int tweetCount;

    public TweetModel() {
        this.model = ModelFactory.createDefaultModel();
        
        model.setNsPrefix("tweet", this.tweetURI);
        model.setNsPrefix("author", this.authorURI);
        model.setNsPrefix("language", this.languageURI);
        model.setNsPrefix("hashtag", this.hashtagURI);
        model.setNsPrefix("topic", this.topicURI);
        model.setNsPrefix("dce", "http://purl.org/dc/elements/1.1/"); //id, date, creator, language
        model.setNsPrefix("vcard", "http://www.w3.org/2001/vcard-rdf/3.0#"); // label, nickname
        model.setNsPrefix("rss", "http://purl.org/rss/1.0/"); // text
        model.setNsPrefix("org", "http://www.w3.org/ns/org#"); // location
        model.setNsPrefix("as", "http://www.w3.org/ns/org#"); // replyTo
        
        this.tweetCount = 1;
    }
    
    public void addResource(Twitter twitter, Tweet tw) {
        User user = tw.getUser();
        Language language_ = tw.getLanguage();
        
        Resource tweet = model.createResource(tweetURI + this.tweetCount++);
        Resource author = model.createResource(authorURI + user.getName());
        Resource language = model.createResource(languageURI + language_.getLabel());
        Resource hashtag = model.createResource(hashtagURI + tw.getHashtag());
        Resource topic = model.createResource(topicURI + tw.getClass_());
        
        tweet.addProperty(DC_11.creator, author);
        tweet.addProperty(DC_11.language, language);
        tweet.addProperty(DC_11.relation, hashtag);
        tweet.addLiteral(DC_11.identifier, tw.getId());
        tweet.addLiteral(DC_11.date, tw.getDate().toString());
        tweet.addLiteral(RSS.textinput, tw.getText());
        
        
        author.addLiteral(VCARD.NICKNAME, user.getName());
        author.addLiteral(ORG.location, user.getLocation());
        
        language.addLiteral(DC_11.identifier, language_.getId());
        language.addLiteral(VCARD.LABEL, language_.getLabel());
        
        hashtag.addProperty(RDF.type, topic);
        
        topic.addLiteral(VCARD.LABEL, tw.getClass_());
        
        checkIfReply(twitter, tw);
    }
    
    public void serializeTurtle() {
        if (!model.isEmpty()) {
            try {
                File file = new File("example.ttl");
                String path = file.getAbsolutePath();
                FileOutputStream fOS = new FileOutputStream(file);
                RDFDataMgr.write(fOS, model, Lang.TURTLE);
                fOS.close();
                JOptionPane.showMessageDialog(null,
                                              "Su fichero de exportación ha sido creado correctamente en " + path,
                                              "Exportación",
                                              JOptionPane.PLAIN_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                                              "Ha ocurrido un error con su fichero de exportación",
                                              "Exportación",
                                              JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(null,
                                              "Antes de exportar debe realizar alguna búsqueda",
                                              "Exportación",
                                              JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void serializeXML() {
         if (!model.isEmpty()) {
            try {
                File file = new File("example.xml");
                String path = file.getAbsolutePath();
                FileOutputStream fOS = new FileOutputStream(file);
                RDFDataMgr.write(fOS, model, Lang.RDFXML);
                fOS.close();
                JOptionPane.showMessageDialog(null,
                                              "Su fichero de exportación ha sido creado correctamente en " + path,
                                              "Exportación",
                                              JOptionPane.PLAIN_MESSAGE);
            }  catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                                              "Ha ocurrido un error con su fichero de exportación",
                                              "Exportación",
                                              JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(null,
                                              "Antes de exportar debe realizar alguna búsqueda",
                                              "Exportación",
                                              JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void checkIfReply(Twitter twitter, Tweet tw) {
        final long replyToId = tw.getReplyToId();
        if (replyToId != -1) {
            try {
                Status new_tweet = twitter.showStatus(replyToId);
                addResource(twitter, new Tweet(
                                     new_tweet.getId(),
                                     -1,
                                     new User(new_tweet.getUser().getName(), new_tweet.getUser().getLocation()),
                                     new Language(new_tweet.getLang(), get_lenguage(new_tweet.getLang())),
                                     new_tweet.getText(),
                                     tw.getClass_(),
                                     tw.getHashtag(),
                                     new_tweet.getCreatedAt()));
            } catch (TwitterException e) {
                System.err.print("Failed to search tweets: " + e.getMessage());
            }
        }
    }
    
    private static String get_lenguage(String iso3){
        for (Locale country : Locale.getAvailableLocales()) {
            if(country.toLanguageTag().equals(iso3))
                return country.getDisplayLanguage();
        }
        return "";
    }
}
