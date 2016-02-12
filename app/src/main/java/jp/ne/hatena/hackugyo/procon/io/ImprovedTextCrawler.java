package jp.ne.hatena.hackugyo.procon.io;

import com.leocardz.link.preview.library.Regex;
import com.leocardz.link.preview.library.SearchUrls;
import com.leocardz.link.preview.library.SourceContent;
import com.leocardz.link.preview.library.TextCrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by kwatanabe on 16/02/12.
 * @see {@link com.leocardz.link.preview.library.TextCrawler}
 */
public class ImprovedTextCrawler {

    public SourceContent extractFrom(String url, int imageQuantity) {
        SourceContent sourceContent = new SourceContent();

        ArrayList<String>  urls = SearchUrls.matches(url);
        if (urls.size() > 0) {
            sourceContent.setFinalUrl(unshortenUrl(TextCrawler.extendedTrim((String) urls.get(0))));
        } else {
            sourceContent.setFinalUrl("");
        }

        if (!sourceContent.getFinalUrl().equals("")) {
            if (isImage(sourceContent.getFinalUrl()) && !sourceContent.getFinalUrl().contains("dropbox")) {
                sourceContent.setSuccess(true);
                sourceContent.getImages().add(sourceContent.getFinalUrl());
                sourceContent.setTitle("");
                sourceContent.setDescription("");
            } else {
                try {
                    Document finalLinkSet = Jsoup.connect(sourceContent.getFinalUrl()).userAgent("Mozilla").get();
                    sourceContent.setHtmlCode(TextCrawler.extendedTrim(finalLinkSet.toString()));
                    HashMap<String, String> metaTags = getMetaTags(sourceContent.getHtmlCode());
                    sourceContent.setMetaTags(metaTags);
                    sourceContent.setTitle((String) metaTags.get("title"));
                    sourceContent.setDescription((String) metaTags.get("description"));
                    if (sourceContent.getTitle().equals("")) {
                        String matchTitle = Regex.pregMatch(sourceContent.getHtmlCode(), "<title(.*?)>(.*?)</title>", 2);
                        if (!matchTitle.equals("")) {
                            sourceContent.setTitle(htmlDecode(matchTitle));
                        }
                    }

                    if (sourceContent.getDescription().equals("")) {
                        sourceContent.setDescription(crawlCode(sourceContent.getHtmlCode()));
                    }

                    sourceContent.setDescription(sourceContent.getDescription().replaceAll("<script(.*?)>(.*?)</script>", ""));
                    if (imageQuantity != -2) {
                        if (!((String) metaTags.get("image")).equals("")) {
                            sourceContent.getImages().add(metaTags.get("image"));
                        } else {
                            sourceContent.setImages(getImages(finalLinkSet, imageQuantity));
                        }
                    }

                    sourceContent.setSuccess(true);
                } catch (Exception var5) {
                    sourceContent.setSuccess(false);
                }
            }
        }

        String[] finalLinkSet1 = sourceContent.getFinalUrl().split("&");
        sourceContent.setUrl(finalLinkSet1[0]);
        sourceContent.setCannonicalUrl(cannonicalPage(sourceContent.getFinalUrl()));
        sourceContent.setDescription(stripTags(sourceContent.getDescription()));
        return sourceContent;
    }


    private String unshortenUrl(String shortURL) {
        if(!shortURL.startsWith("http://") && !shortURL.startsWith("https://")) {
            return "";
        } else {
            URLConnection urlConn = this.connectURL(shortURL);
            urlConn.getHeaderFields();
            String finalResult = urlConn.getURL().toString();
            urlConn = this.connectURL(finalResult);
            urlConn.getHeaderFields();

            for(shortURL = urlConn.getURL().toString(); !shortURL.equals(finalResult); finalResult = this.unshortenUrl(finalResult)) {
                ;
            }

            return finalResult;
        }
    }

    private URLConnection connectURL(String strURL) {
        URLConnection conn = null;

        try {
            URL ioe = new URL(strURL);
            conn = ioe.openConnection();
        } catch (MalformedURLException var4) {
            System.out.println("Please input a valid URL");
        } catch (IOException var5) {
            System.out.println("Can not connect to the URL");
        }

        return conn;
    }

    public static String extendedTrim(String content) {
        return content.replaceAll("\\s+", " ").replace("\n", " ").replace("\r", " ").trim();
    }


    private String getTagContent(String tag, String content) {
        String pattern = "<" + tag + "(.*?)>(.*?)</" + tag + ">";
        String result = "";
        String currentMatch = "";
        List matches = Regex.pregMatchAll(content, pattern, 2);

        for(int matchFinal = 0; matchFinal < matches.size(); ++matchFinal) {
            currentMatch = this.stripTags((String)matches.get(matchFinal));
            if(currentMatch.length() >= 120) {
                result = extendedTrim(currentMatch);
                break;
            }
        }

        if(result.equals("")) {
            String var8 = Regex.pregMatch(content, pattern, 2);
            result = extendedTrim(var8);
        }

        result = result.replaceAll("&nbsp;", "");
        return this.htmlDecode(result);
    }

    public List<String> getImages(Document document, int imageQuantity) {
        Object matches = new ArrayList();
        Elements media = document.select("[src]");
        Iterator i$ = media.iterator();

        while(i$.hasNext()) {
            Element srcElement = (Element)i$.next();
            if(srcElement.tagName().equals("img")) {
                ((List)matches).add(srcElement.attr("abs:src"));
            }
        }

        if(imageQuantity != -1) {
            matches = ((List)matches).subList(0, imageQuantity);
        }

        return (List)matches;
    }

    private String htmlDecode(String content) {
        return Jsoup.parse(content).text();
    }

    private String crawlCode(String content) {
        String result = "";
        String resultSpan = "";
        String resultParagraph = "";
        String resultDiv = "";
        resultSpan = this.getTagContent("span", content);
        resultParagraph = this.getTagContent("p", content);
        resultDiv = this.getTagContent("div", content);
        if(resultParagraph.length() > resultSpan.length() && resultParagraph.length() >= resultDiv.length()) {
            result = resultParagraph;
        } else if(resultParagraph.length() > resultSpan.length() && resultParagraph.length() < resultDiv.length()) {
            result = resultDiv;
        } else {
            result = resultParagraph;
        }

        return this.htmlDecode(result);
    }

    private String cannonicalPage(String url) {
        String cannonical = "";
        if(url.startsWith("http://")) {
            url = url.substring("http://".length());
        } else if(url.startsWith("https://")) {
            url = url.substring("https://".length());
        }

        for(int i = 0; i < url.length() && url.charAt(i) != 47; ++i) {
            cannonical = cannonical + url.charAt(i);
        }

        return cannonical;
    }

    private String stripTags(String content) {
        return Jsoup.parse(content).text();
    }

    private boolean isImage(String url) {
        return url.matches("(.+?)\\.(jpg|png|gif|bmp)$");
    }

    private HashMap<String, String> getMetaTags(String content) {
        HashMap metaTags = new HashMap();
        metaTags.put("url", "");
        metaTags.put("title", "");
        metaTags.put("description", "");
        metaTags.put("image", "");
        List matches = Regex.pregMatchAll(content, "<meta(.*?)>", 1);
        Iterator i$ = matches.iterator();

        while(true) {
            String match;
            label62:
            do {
                while(true) {
                    while(true) {
                        while(i$.hasNext()) {
                            match = (String)i$.next();
                            if(!match.toLowerCase().contains("property=\"og:url\"") && !match.toLowerCase().contains("property=\'og:url\'") && !match.toLowerCase().contains("name=\"url\"") && !match.toLowerCase().contains("name=\'url\'")) {
                                if(!match.toLowerCase().contains("property=\"og:title\"") && !match.toLowerCase().contains("property=\'og:title\'") && !match.toLowerCase().contains("name=\"title\"") && !match.toLowerCase().contains("name=\'title\'")) {
                                    if(!match.toLowerCase().contains("property=\"og:description\"") && !match.toLowerCase().contains("property=\'og:description\'") && !match.toLowerCase().contains("name=\"description\"") && !match.toLowerCase().contains("name=\'description\'")) {
                                        continue label62;
                                    }

                                    metaTags.put("description", this.separeMetaTagsContent(match));
                                } else {
                                    metaTags.put("title", this.separeMetaTagsContent(match));
                                }
                            } else {
                                metaTags.put("url", this.separeMetaTagsContent(match));
                            }
                        }

                        return metaTags;
                    }
                }
            } while(!match.toLowerCase().contains("property=\"og:image\"") && !match.toLowerCase().contains("property=\'og:image\'") && !match.toLowerCase().contains("name=\"image\"") && !match.toLowerCase().contains("name=\'image\'"));

            metaTags.put("image", this.separeMetaTagsContent(match));
        }
    }

    private String separeMetaTagsContent(String content) {
        String result = Regex.pregMatch(content, "content=\"(.*?)\"", 1);
        return this.htmlDecode(result);
    }
}
