import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlPage
import java.util.regex.Pattern
import net.sf.json.JSONObject
import hudson.util.VersionNumber

def listUp(url,pattern) {
    wc = new WebClient()
    wc.setJavaScriptEnabled(false);
    wc.setCssEnabled(false);
    HtmlPage p = wc.getPage(url);
    pattern=Pattern.compile(pattern);

    return p.getAnchors().collect { HtmlAnchor a ->
        m = pattern.matcher(a.hrefAttribute)
        if(m.find()) {
            ver=m.group(1)
            url = p.getFullyQualifiedUrl(a.hrefAttribute);
            return ["id":ver, "name":ver, "url":url.toExternalForm()]
        }
        return null;
    }.findAll { it!=null }.sort { o1,o2 ->
        try {
            def v1 = new VersionNumber(o1.id)
            try {
                new VersionNumber(o2.id).compareTo(v1)
            } catch (IllegalArgumentException _2) {
                -1
            }
        } catch (IllegalArgumentException _1) {
            try {
                new VersionNumber(o2.id)
                1
            } catch (IllegalArgumentException _2) {
                o2.id.compareTo(o1.id)
            }
        }
    }
}

def store(key,o) {
    JSONObject envelope = JSONObject.fromObject(["list": o]);
    println envelope.toString(2)

    if(project!=null) {
        // if we run from GMaven during a build, put that out in a file as well, with the JSONP support
        File d = new File(project.basedir, "target")
        d.mkdirs()
        new File(d,"${key}.json").write("downloadService.post('${key}',${envelope.toString()})");
    }
}


store("hudson.tasks.Ant.AntInstaller",  listUp("http://archive.apache.org/dist/ant/binaries/",  "ant-([0-9.]+)-bin.zip\$"))
store("hudson.tasks.Maven.MavenInstaller",listUp("http://archive.apache.org/dist/maven/binaries/","maven-([0-9.]+)(-bin)?.zip\$"))
