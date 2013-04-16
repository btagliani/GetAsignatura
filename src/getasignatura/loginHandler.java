/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package getasignatura;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import org.htmlparser.*;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.LinkRegexFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.jdesktop.application.Application;

/**
 *
 * @author Bruno
 */
public class loginHandler implements Runnable {

    private String url = "http://webasignatura.ucu.edu.uy/login/index.php";
    private String user;
    private String pass;
    private String path;
    private HashMap<String, String> data;
    private CookieManager manager;
    private boolean loginResult = false;
    private boolean doDownload = false;
    private boolean windows = true;
    private float tamañoDescarga = 0;
    private String materiaDescarga = "";
    private String archivosDescarga = "";
    private String comentarios = "";
    private String carpeta = "";
    private ArrayList<String> materias;

    public void run() {
        try {
            if (!doDownload) {
                loginResult = this.doLogin();
            }
            if (loginResult && doDownload) {
                this.doDownload();
                GetAsignaturaApp temp = Application.getInstance(GetAsignaturaApp.class);
                JOptionPane.showMessageDialog(temp.getMainFrame().getContentPane(), "Descarga Completada", "Descarga", JOptionPane.INFORMATION_MESSAGE);
                deleteEmptyFolders(path);
            }
        } catch (Exception ex) {
            System.out.println(ex);
            GetAsignaturaApp temp = Application.getInstance(GetAsignaturaApp.class);
            JOptionPane.showMessageDialog(temp.getMainFrame().getContentPane(), "La descarga se detuvo por el siguente error, por favor enviar un mail a bruno.taglian@gmail.com con el siguente mensaje de error para que pueda ser solucionado " + ex.getMessage(), "Download Error", JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(loginHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public loginHandler(String user, String pass) {
        this.user = user;
        this.pass = pass;
        this.data = new HashMap<String, String>();
        data.put("username", this.user);
        data.put("password", this.pass);
        manager = new CookieManager();
    }

    public boolean isLogedIn() {
        return loginResult;
    }

    public void setDoDownload(boolean doDownload) {
        this.doDownload = doDownload;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setMaterias(ArrayList<String> materias) {
        this.materias = materias;
    }

    public void setWindows(boolean windows) {
        this.windows = windows;
    }

    public ArrayList<String> getMaterias() {
        return materias;
    }

    public String getArchivosDescarga() {
        return archivosDescarga;
    }

    public String getMateriaDescarga() {
        return materiaDescarga;
    }

    public float getTamañoDescarga() {
        return tamañoDescarga;
    }

    public String getComentarios() {
        return comentarios;
    }

    public String getCarpeta() {
        return carpeta;
    }

    public synchronized boolean doLogin() throws Exception {
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(manager);
        URL siteUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) siteUrl.openConnection();
        conn.setRequestProperty("Cookie", retrieveCookie(url));
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
        Set keys = data.keySet();
        Iterator keyIter = keys.iterator();
        String content = "";
        for (int i = 0; keyIter.hasNext(); i++) {
            Object key = keyIter.next();
            if (i != 0) {
                content += "&";
            }
            content += key + "=" + URLEncoder.encode(data.get(key), "UTF-8");
        }
        content += "&" + "testcookies" + "=" + URLEncoder.encode("1", "UTF-8");
        out.writeBytes(content);
        out.flush();
        out.close();
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line = "";
        while ((line = in.readLine()) != null) {
            if (line.contains("Datos")) {
                in.close();
                return false;
            }
        }
        NodeFilter f = new HasAttributeFilter("title", "Haga clic aquí para entrar al curso");
        NodeList html2node = html2node("http://webasignatura.ucu.edu.uy/", f);
        ArrayList<String> node2list = node2list(html2node);
        if (node2list.isEmpty()) {
            NodeFilter eng = new HasAttributeFilter("title", "Click to enter this course");
            html2node = html2node("http://webasignatura.ucu.edu.uy/", eng);
            node2list = node2list(html2node);
        }
        materias = new ArrayList<String>();
        for (String link : node2list) {
            materias.add(data.get(link));
        }
        in.close();
        return true;
    }

    public void doDownload() {
        try {
            ArrayList<String> links = new ArrayList<String>();
            if (!materias.isEmpty()) {
                for (String mat : materias) {
                    for (String link : data.keySet()) {
                        if (data.get(link).equals(mat)) {
                            links.add(link);
                        }
                    }
                }
                downloadFolder(links, path);
            } else {
                NodeFilter f = new HasAttributeFilter("title", "Haga clic aquí para entrar al curso");
                NodeList html2node = html2node("http://webasignatura.ucu.edu.uy/", f);
                ArrayList<String> node2list = node2list(html2node);
                if (node2list.isEmpty()) {
                    NodeFilter eng = new HasAttributeFilter("title", "Click to enter this course");
                    html2node = html2node("http://webasignatura.ucu.edu.uy/", eng);
                    node2list = node2list(html2node);
                }
                materias = new ArrayList<String>();
                for (String link : node2list) {
                    materias.add(data.get(link));
                }
                for (String mat : materias) {
                    for (String link : data.keySet()) {
                        if (data.get(link).equals(mat)) {
                            links.add(link);
                        }
                    }
                }
                downloadFolder(links, path);
            }
            
        } catch (MalformedURLException ex) {
            Logger.getLogger(loginHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(loginHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserException ex) {
            Logger.getLogger(loginHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    String retrieveCookie(String url) throws IOException {
        URL site = new URL(url);
        HttpURLConnection cook = (HttpURLConnection) site.openConnection();
        Map<String, List<String>> headers = cook.getHeaderFields();
        List<String> values = headers.get("Set-Cookie");
        String cookieValue = null;
        for (Iterator iter = values.iterator(); iter.hasNext();) {
            String v = (String) iter.next();
            if (cookieValue == null) {
                cookieValue = v;
            } else {
                cookieValue = cookieValue + ";" + v;
            }
        }
        return cookieValue;
    }

    void downloadFolder(ArrayList<String> list, String pathNow) throws MalformedURLException, IOException, ParserException {
        ArrayList<String> listaFolder = new ArrayList<String>();
        String pathTemp = pathNow;
        for (String urlFolder : list) {
            if (data.containsKey(urlFolder)) {
                String nombreMateria = data.get(urlFolder).trim();
                if (!windows) {
                    nombreMateria = limpiar(nombreMateria);
                }
                createFolder(pathTemp, nombreMateria);
                if (windows) {
                    pathTemp += nombreMateria + "\\";
                } else {
                    pathTemp += nombreMateria + "/";
                }
                if (materias.contains(data.get(urlFolder))) {
                    materiaDescarga = data.get(urlFolder);
                }
            } else {
                String asd = urlFolder.replaceFirst("http://webasignatura.ucu.edu.uy/mod/resource/", "");
                asd = asd.replaceFirst("&", "&amp;");
                if (!windows){
                    asd = limpiar(asd);
                }
                createFolder(pathTemp, data.get(asd).trim());
                if (windows) {
                    pathTemp += data.get(asd).trim() + "\\";
                } else {
                    pathTemp += data.get(asd).trim() + "/";
                }
            }
            if (!urlFolder.startsWith("http")) {
                urlFolder = "http://webasignatura.ucu.edu.uy/mod/resource/" + urlFolder;
            }
            NodeFilter f = new LinkRegexFilter("resource");
            NodeFilter f2 = new LinkRegexFilter("index");
            NodeFilter f3 = new NotFilter(f2);
            NodeList html2node = html2node(urlFolder, f);
            listaFolder = node2list(html2node.extractAllNodesThatMatch(f3));
            ArrayList<String> listaFinal = new ArrayList<String>();
            String parent = urlFolder.replaceAll("&subdir.*", "");
            for (String asd : listaFolder) {
                if (!asd.startsWith("http")) {
                    String qwe = "http://webasignatura.ucu.edu.uy/mod/resource/" + asd;
                    listaFinal.add(qwe.replaceAll("amp;", ""));
                } else {
                    if (!asd.contains("inpopup")) {
                        listaFinal.add(asd);
                    }
                }
                if (listaFinal.contains(parent)) {
                    listaFinal.remove(parent);
                }
            }
            downloadFile(urlFolder, pathTemp);
            if (!listaFinal.isEmpty() && !listaFinal.equals(list)) {
                downloadFolder(listaFinal, pathTemp);
            }
            pathTemp = pathNow;
        }

    }

    void downloadFile(String urlFiles, String pathDown) throws ParserException, MalformedURLException, IOException {
        NodeFilter f = new LinkRegexFilter("file.php");
        ArrayList<String> list = node2list(html2node(urlFiles, f));
        for (String file : list) {
            String filed = data.get(file);
            String nombre = filed.replaceAll("&nbsp", "");
            nombre = nombre.replaceAll(";", "");
            archivosDescarga = (nombre);
            carpeta = pathDown;
            URL urlFile = new URL(file);
            if (fileExists(pathDown + nombre)) {
                long t = new File(pathDown + nombre).lastModified();
                Date fileDate = new Date(t);
                URLConnection fileCon = urlFile.openConnection();
                Date date = new Date(fileCon.getLastModified());
                int modificado = fileDate.compareTo(date);
                Long fileex = new File(pathDown + nombre).length();
                Long fileurl = fileCon.getContentLengthLong();
                if (modificado < 0 || !fileex.equals(fileurl)) {
                    comentarios = "Descargando archivo actualizado";
                    tamañoDescarga += fileurl;
                    java.io.BufferedInputStream in = new java.io.BufferedInputStream(urlFile.openStream());
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(pathDown + nombre);
                    java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
                    byte[] filedata = new byte[1024];
                    int x = 0;
                    while ((x = in.read(filedata, 0, 1024)) >= 0) {
                        bout.write(filedata, 0, x);
                    }
                    bout.close();
                    fos.close();
                    in.close();
                } else {
                    comentarios = "El archivo existe y es igual al de la web, no se descargará";
                }
            } else {
                comentarios = "Descargando";
                try {
                    createFolder(pathDown);
                    java.io.BufferedInputStream in = new java.io.BufferedInputStream(urlFile.openStream());
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(pathDown + nombre);
                    java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
                    byte[] filedata = new byte[1024];
                    int x = 0;
                    while ((x = in.read(filedata, 0, 1024)) >= 0) {
                        bout.write(filedata, 0, x);
                    }
                    bout.close();
                    fos.close();
                    in.close();
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
                tamañoDescarga += new File(pathDown + nombre).length();

            }
        }
    }

    ArrayList<String> node2list(NodeList list) throws IOException {
        String link = "";
        String nombre = "";
        String c = "";
        ArrayList<String> links = new ArrayList<String>();
        Pattern p = Pattern.compile("href=\"(.*)\">");
        Pattern a = Pattern.compile("file.php");
        for (Node node : list.toNodeArray()) {
            link = node.toHtml();
            nombre = node.toPlainTextString().trim();
            Matcher m = p.matcher(link);
            Matcher n = a.matcher(link);
            if (m.find()) {
                c = m.group(1);
                if (n.find()) {
                    p = Pattern.compile("href=\"(.*)\" *onclick");
                    m = p.matcher(link);
                    if (m.find()) {
                        c = m.group(1);
                    }
                }
                links.add(c);
                if (nombre.contains("%2F")) {
                    int last = nombre.lastIndexOf("%2F");
                    nombre = nombre.substring(last + 3, nombre.length());
                }
                nombre = nombre.replaceAll("&nbsp", "");
                nombre = nombre.replaceAll(";", "");
                nombre = nombre.replaceAll(":", "");
                nombre = nombre.replaceAll("|", "");
                nombre = nombre.replaceAll("\"", "");
                nombre = nombre.replaceAll("!", "");
                nombre = nombre.replaceAll("archivo", "");
                nombre = nombre.replaceAll("PDF", "");
                nombre = nombre.replaceAll("documento", "");
                nombre = nombre.replaceAll("_", " ");
                nombre = nombre.trim();
                data.put(c, nombre);
            }
        }
        return links;
    }

    NodeList html2node(String urlHTML, NodeFilter f) throws MalformedURLException, IOException {
        NodeList lista = new NodeList();
        try {
            URL urlnode = new URL(urlHTML);
            URLConnection nodeCon = urlnode.openConnection();
            InputStream inputStream = nodeCon.getInputStream();
            Parser parse = new Parser(nodeCon);
            lista = parse.extractAllNodesThatMatch(f);
        } catch (FileNotFoundException filenotfoundexxption) {
            System.out.println("File not found");
        } catch (IOException e) {
            System.out.println("File error " + e.getMessage());
        } catch (ParserException ex) {
            Logger.getLogger(loginHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return lista;
    }

    void createFolder(String path, String name) {
        File f = new File(path + name.trim());
        f.mkdir();
    }

    void createFolder(String path) {
        File f = new File(path);
        f.mkdir();
    }

    boolean fileExists(String path) {
        File f = new File(path);
        return f.exists();
    }

    public String limpiar(String input) {

        // Cadena de caracteres original a sustituir.

        String original = "áàäéèëíìïóòöúùuñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ";

        // Cadena de caracteres ASCII que reemplazarán los originales.

        String ascii = "aaaeeeiiiooouuunAAAEEEIIIOOOUUUNcC";

        String output = input;

        for (int i = 0; i < original.length(); i++) {

            // Reemplazamos los caracteres especiales.

            output = output.replace(original.charAt(i), ascii.charAt(i));

        }

        return output;

    }

    public static void deleteEmptyFolders(String folderName) throws FileNotFoundException {
        File aStartingDir = new File(folderName);
        List<File> emptyFolders = new ArrayList<File>();
        findEmptyFoldersInDir(aStartingDir, emptyFolders);
        List<String> fileNames = new ArrayList<String>();
        for (File f : emptyFolders) {
            String s = f.getAbsolutePath();
            fileNames.add(s);
        }
        for (File f : emptyFolders) {
            boolean isDeleted = f.delete();
            if (isDeleted) {
                System.out.println(f.getPath() + " deleted");
            }
        }
    }

    public static boolean findEmptyFoldersInDir(File folder, List<File> emptyFolders) {
        boolean isEmpty = false;
        File[] filesAndDirs = folder.listFiles();
        List<File> filesDirs = new ArrayList<File>();
        if (filesAndDirs != null) {
            filesDirs = Arrays.asList(filesAndDirs);
        }
        if (filesDirs.size() == 0) {
            isEmpty = true;
        }
        if (filesDirs.size() > 0) {
            boolean allDirsEmpty = true;
            boolean noFiles = true;
            for (File file : filesDirs) {
                if (!file.isFile()) {
                    boolean isEmptyChild = findEmptyFoldersInDir(file, emptyFolders);
                    if (!isEmptyChild) {
                        allDirsEmpty = false;
                    }
                }
                if (file.isFile()) {
                    noFiles = false;
                }
            }
            if (noFiles == true && allDirsEmpty == true) {
                isEmpty = true;
            }
        }
        if (isEmpty) {
            emptyFolders.add(folder);
        }
        return isEmpty;
    }
}
