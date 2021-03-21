package co.casterlabs.rakurai.io.http;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import lombok.NonNull;

public class MimeTypes {
    public static final Map<String, String> mimeTypes = new HashMap<>();

    static {
        mimeTypes.put("3dm", "x-world/x-3dmf");
        mimeTypes.put("3dmf", "x-world/x-3dmf");
        mimeTypes.put("aab", "application/x-authorware-bin");
        mimeTypes.put("aam", "application/x-authorware-map");
        mimeTypes.put("aac", "audio/aac");
        mimeTypes.put("aas", "application/x-authorware-seg");
        mimeTypes.put("abc", "text/vnd.abc");
        mimeTypes.put("acgi", "text/html");
        mimeTypes.put("afl", "video/animaflex");
        mimeTypes.put("ai", "application/postscript");
        mimeTypes.put("aif", "audio/aiff");
        mimeTypes.put("aifc", "audio/aiff");
        mimeTypes.put("aiff", "audio/aiff");
        mimeTypes.put("aim", "application/x-aim");
        mimeTypes.put("aip", "text/x-audiosoft-intra");
        mimeTypes.put("ani", "application/x-navi-animation");
        mimeTypes.put("aos", "application/x-nokia-9000-communicator-add-on-software");
        mimeTypes.put("aps", "application/mime");
        mimeTypes.put("arj", "application/arj");
        mimeTypes.put("art", "image/x-jg");
        mimeTypes.put("asf", "video/x-ms-asf");
        mimeTypes.put("asm", "text/x-asm");
        mimeTypes.put("asp", "text/asp");
        mimeTypes.put("au", "audio/basic");
        mimeTypes.put("avi", "video/avi");
        mimeTypes.put("avs", "video/avs-video");
        mimeTypes.put("bcpio", "application/x-bcpio");
        mimeTypes.put("bin", "application/octet-stream");
        mimeTypes.put("bm", "image/bmp");
        mimeTypes.put("bmp", "image/bmp");
        mimeTypes.put("boo", "application/book");
        mimeTypes.put("book", "application/book");
        mimeTypes.put("boz", "application/x-bzip2");
        mimeTypes.put("bsh", "application/x-bsh");
        mimeTypes.put("bz", "application/x-bzip");
        mimeTypes.put("bz2", "application/x-bzip2");
        mimeTypes.put("c", "text/plain");
        mimeTypes.put("c++", "text/plain");
        mimeTypes.put("cat", "application/vnd.ms-pki.seccat");
        mimeTypes.put("cc", "text/plain");
        mimeTypes.put("ccad", "application/clariscad");
        mimeTypes.put("cco", "application/x-cocoa");
        mimeTypes.put("cdf", "application/cdf");
        mimeTypes.put("cer", "application/pkix-cert");
        mimeTypes.put("cha", "application/x-chat");
        mimeTypes.put("chat", "application/x-chat");
        mimeTypes.put("class", "application/java-byte-code");
        mimeTypes.put("com", "application/octet-stream");
        mimeTypes.put("conf", "text/plain");
        mimeTypes.put("cpio", "application/x-cpio");
        mimeTypes.put("cpt", "application/mac-compactpro");
        mimeTypes.put("crl", "application/pkcs-crl");
        mimeTypes.put("crt", "application/x-x509-ca-cert");
        mimeTypes.put("csh", "application/x-csh");
        mimeTypes.put("css", "text/css");
        mimeTypes.put("cxx", "text/plain");
        mimeTypes.put("dcr", "application/x-director");
        mimeTypes.put("deepv", "application/x-deepv");
        mimeTypes.put("def", "text/plain");
        mimeTypes.put("der", "application/x-x509-ca-cert");
        mimeTypes.put("dif", "video/x-dv");
        mimeTypes.put("dir", "application/x-director");
        mimeTypes.put("dl", "video/dl");
        mimeTypes.put("doc", "application/msword");
        mimeTypes.put("dot", "application/msword");
        mimeTypes.put("dp", "application/commonground");
        mimeTypes.put("drw", "application/drafting");
        mimeTypes.put("dump", "application/octet-stream");
        mimeTypes.put("dv", "video/x-dv");
        mimeTypes.put("dvi", "application/x-dvi");
        mimeTypes.put("dwf", "model/vnd.dwf");
        mimeTypes.put("dwg", "application/acad");
        mimeTypes.put("dxf", "application/dxf");
        mimeTypes.put("dxr", "application/x-director");
        mimeTypes.put("el", "text/x-script.elisp");
        mimeTypes.put("elc", "application/x-elc");
        mimeTypes.put("env", "application/x-envoy");
        mimeTypes.put("eps", "application/postscript");
        mimeTypes.put("es", "application/x-esrehber");
        mimeTypes.put("etx", "text/x-setext");
        mimeTypes.put("evy", "application/envoy");
        mimeTypes.put("exe", "application/octet-stream");
        mimeTypes.put("f", "text/plain");
        mimeTypes.put("f77", "text/plain");
        mimeTypes.put("f90", "text/plain");
        mimeTypes.put("fdf", "application/vnd.fdf");
        mimeTypes.put("fif", "image/fif");
        mimeTypes.put("fli", "video/fli");
        mimeTypes.put("flo", "image/florian");
        mimeTypes.put("flx", "text/vnd.fmi.flexstor");
        mimeTypes.put("fmf", "video/x-atomic3d-feature");
        mimeTypes.put("for", "text/plain");
        mimeTypes.put("fpx", "image/vnd.fpx");
        mimeTypes.put("frl", "application/freeloader");
        mimeTypes.put("funk", "audio/make");
        mimeTypes.put("g", "text/plain");
        mimeTypes.put("g3", "image/g3fax");
        mimeTypes.put("gif", "image/gif");
        mimeTypes.put("gl", "video/gl");
        mimeTypes.put("gsd", "audio/x-gsm");
        mimeTypes.put("gsm", "audio/x-gsm");
        mimeTypes.put("gsp", "application/x-gsp");
        mimeTypes.put("gss", "application/x-gss");
        mimeTypes.put("gtar", "application/x-gtar");
        mimeTypes.put("gz", "application/x-gzip");
        mimeTypes.put("gzip", "application/x-gzip");
        mimeTypes.put("h", "text/plain");
        mimeTypes.put("hdf", "application/x-hdf");
        mimeTypes.put("help", "application/x-helpfile");
        mimeTypes.put("hgl", "application/vnd.hp-hpgl");
        mimeTypes.put("hh", "text/plain");
        mimeTypes.put("hlp", "application/hlp");
        mimeTypes.put("hpg", "application/vnd.hp-hpgl");
        mimeTypes.put("hpgl", "application/vnd.hp-hpgl");
        mimeTypes.put("hqx", "application/binhex");
        mimeTypes.put("hta", "application/hta");
        mimeTypes.put("htc", "text/x-component");
        mimeTypes.put("htm", "text/html");
        mimeTypes.put("html", "text/html");
        mimeTypes.put("htmls", "text/html");
        mimeTypes.put("htt", "text/webviewhtml");
        mimeTypes.put("htx", "text/html");
        mimeTypes.put("ice", "x-conference/x-cooltalk");
        mimeTypes.put("ico", "image/x-icon");
        mimeTypes.put("idc", "text/plain");
        mimeTypes.put("ief", "image/ief");
        mimeTypes.put("iefs", "image/ief");
        mimeTypes.put("iges", "application/iges");
        mimeTypes.put("igs", "application/iges");
        mimeTypes.put("ima", "application/x-ima");
        mimeTypes.put("imap", "application/x-httpd-imap");
        mimeTypes.put("inf", "application/inf");
        mimeTypes.put("ins", "application/x-internett-signup");
        mimeTypes.put("ip", "application/x-ip2");
        mimeTypes.put("isu", "video/x-isvideo");
        mimeTypes.put("it", "audio/it");
        mimeTypes.put("iv", "application/x-inventor");
        mimeTypes.put("ivr", "i-world/i-vrml");
        mimeTypes.put("ivy", "application/x-livescreen");
        mimeTypes.put("jam", "audio/x-jam");
        mimeTypes.put("jav", "text/plain");
        mimeTypes.put("java", "text/plain");
        mimeTypes.put("jcm", "application/x-java-commerce");
        mimeTypes.put("jfif", "image/jpeg");
        mimeTypes.put("jfif-tbnl", "image/jpeg");
        mimeTypes.put("jpe", "image/jpeg");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("jps", "image/x-jps");
        mimeTypes.put("js", "application/javascript");
        mimeTypes.put("json", "application/json");
        mimeTypes.put("jut", "image/jutvision");
        mimeTypes.put("kar", "music/x-karaoke");
        mimeTypes.put("ksh", "application/x-ksh");
        mimeTypes.put("la", "audio/nspaudio");
        mimeTypes.put("lam", "audio/x-liveaudio");
        mimeTypes.put("latex", "application/x-latex");
        mimeTypes.put("lha", "application/lha");
        mimeTypes.put("list", "text/plain");
        mimeTypes.put("lma", "audio/nspaudio");
        mimeTypes.put("log", "text/plain");
        mimeTypes.put("lsp", "application/x-lisp");
        mimeTypes.put("lst", "text/plain");
        mimeTypes.put("lsx", "text/x-la-asf");
        mimeTypes.put("ltx", "application/x-latex");
        mimeTypes.put("lzh", "application/x-lzh");
        mimeTypes.put("lzx", "application/lzx");
        mimeTypes.put("m", "text/plain");
        mimeTypes.put("m1v", "video/mpeg");
        mimeTypes.put("m2a", "audio/mpeg");
        mimeTypes.put("m2v", "video/mpeg");
        mimeTypes.put("m3u", "audio/x-mpequrl");
        mimeTypes.put("man", "application/x-troff-man");
        mimeTypes.put("map", "application/x-navimap");
        mimeTypes.put("mar", "text/plain");
        mimeTypes.put("mbd", "application/mbedlet");
        mimeTypes.put("mc$", "application/x-magic-cap-package-1.0");
        mimeTypes.put("mcd", "application/mcad");
        mimeTypes.put("mcf", "text/mcf");
        mimeTypes.put("mcp", "application/netmc");
        mimeTypes.put("md", "text/plain");
        mimeTypes.put("me", "application/x-troff-me");
        mimeTypes.put("mht", "message/rfc822");
        mimeTypes.put("mhtml", "message/rfc822");
        mimeTypes.put("mid", "audio/midi");
        mimeTypes.put("midi", "audio/midi");
        mimeTypes.put("mif", "application/x-mif");
        mimeTypes.put("mime", "www/mime");
        mimeTypes.put("mjf", "audio/x-vnd.audioexplosion.mjuicemediafile");
        mimeTypes.put("mjpg", "video/x-motion-jpeg");
        mimeTypes.put("mjs", "  text/javascript");
        mimeTypes.put("mm", "application/base64");
        mimeTypes.put("mme", "application/base64");
        mimeTypes.put("mod", "audio/mod");
        mimeTypes.put("moov", "video/quicktime");
        mimeTypes.put("mov", "video/quicktime");
        mimeTypes.put("movie", "video/x-sgi-movie");
        mimeTypes.put("mp2", "audio/mpeg");
        mimeTypes.put("mp3", "audio/mpeg3");
        mimeTypes.put("mp4", "video/mp4");
        mimeTypes.put("mpa", "audio/mpeg");
        mimeTypes.put("mpc", "application/x-project");
        mimeTypes.put("mpe", "video/mpeg");
        mimeTypes.put("mpeg", "video/mpeg");
        mimeTypes.put("mpg", "video/mpeg");
        mimeTypes.put("mpga", "audio/mpeg");
        mimeTypes.put("mpp", "application/vnd.ms-project");
        mimeTypes.put("mpt", "application/x-project");
        mimeTypes.put("mpv", "application/x-project");
        mimeTypes.put("mpx", "application/x-project");
        mimeTypes.put("mrc", "application/marc");
        mimeTypes.put("ms", "application/x-troff-ms");
        mimeTypes.put("mv", "video/x-sgi-movie");
        mimeTypes.put("my", "audio/make");
        mimeTypes.put("mzz", "application/x-vnd.audioexplosion.mzz");
        mimeTypes.put("nap", "image/naplps");
        mimeTypes.put("naplps", "image/naplps");
        mimeTypes.put("nc", "application/x-netcdf");
        mimeTypes.put("ncm", "application/vnd.nokia.configuration-message");
        mimeTypes.put("nif", "image/x-niff");
        mimeTypes.put("niff", "image/x-niff");
        mimeTypes.put("nix", "application/x-mix-transfer");
        mimeTypes.put("nsc", "application/x-conference");
        mimeTypes.put("nvd", "application/x-navidoc");
        mimeTypes.put("oda", "application/oda");
        mimeTypes.put("omc", "application/x-omc");
        mimeTypes.put("omcd", "application/x-omcdatamaker");
        mimeTypes.put("omcr", "application/x-omcregerator");
        mimeTypes.put("p", "text/x-pascal");
        mimeTypes.put("p10", "application/pkcs10");
        mimeTypes.put("p12", "application/pkcs-12");
        mimeTypes.put("p7a", "application/x-pkcs7-signature");
        mimeTypes.put("p7c", "application/pkcs7-mime");
        mimeTypes.put("p7m", "application/pkcs7-mime");
        mimeTypes.put("p7r", "application/x-pkcs7-certreqresp");
        mimeTypes.put("p7s", "application/pkcs7-signature");
        mimeTypes.put("part", "application/pro_eng");
        mimeTypes.put("pas", "text/pascal");
        mimeTypes.put("pbm", "image/x-portable-bitmap");
        mimeTypes.put("pcl", "application/x-pcl");
        mimeTypes.put("pct", "image/x-pict");
        mimeTypes.put("pcx", "image/x-pcx");
        mimeTypes.put("pdb", "chemical/x-pdb");
        mimeTypes.put("pdf", "application/pdf");
        mimeTypes.put("pfunk", "audio/make");
        mimeTypes.put("pgm", "image/x-portable-graymap");
        mimeTypes.put("pic", "image/pict");
        mimeTypes.put("pict", "image/pict");
        mimeTypes.put("pkg", "application/x-newton-compatible-pkg");
        mimeTypes.put("pko", "application/vnd.ms-pki.pko");
        mimeTypes.put("pl", "text/plain");
        mimeTypes.put("plx", "application/x-pixclscript");
        mimeTypes.put("pm", "image/x-xpixmap");
        mimeTypes.put("pm4", "application/x-pagemaker");
        mimeTypes.put("pm5", "application/x-pagemaker");
        mimeTypes.put("png", "image/png");
        mimeTypes.put("pnm", "application/x-portable-anymap");
        mimeTypes.put("pot", "application/mspowerpoint");
        mimeTypes.put("pov", "model/x-pov");
        mimeTypes.put("ppa", "application/vnd.ms-powerpoint");
        mimeTypes.put("ppm", "image/x-portable-pixmap");
        mimeTypes.put("pps", "application/mspowerpoint");
        mimeTypes.put("ppt", "application/mspowerpoint");
        mimeTypes.put("ppz", "application/mspowerpoint");
        mimeTypes.put("pre", "application/x-freelance");
        mimeTypes.put("prt", "application/pro_eng");
        mimeTypes.put("ps", "application/postscript");
        mimeTypes.put("pvu", "paleovu/x-pv");
        mimeTypes.put("pwz", "application/vnd.ms-powerpoint");
        mimeTypes.put("py", "text/x-script.phyton");
        mimeTypes.put("pyc", "application/x-bytecode.python");
        mimeTypes.put("qcp", "audio/vnd.qcelp");
        mimeTypes.put("qd3", "x-world/x-3dmf");
        mimeTypes.put("qd3d", "x-world/x-3dmf");
        mimeTypes.put("qif", "image/x-quicktime");
        mimeTypes.put("qt", "video/quicktime");
        mimeTypes.put("qtc", "video/x-qtc");
        mimeTypes.put("qti", "image/x-quicktime");
        mimeTypes.put("qtif", "image/x-quicktime");
        mimeTypes.put("ra", "audio/x-realaudio");
        mimeTypes.put("ram", "audio/x-pn-realaudio");
        mimeTypes.put("rar", "  application/vnd.rar");
        mimeTypes.put("ras", "image/cmu-raster");
        mimeTypes.put("rast", "image/cmu-raster");
        mimeTypes.put("rexx", "text/x-script.rexx");
        mimeTypes.put("rf", "image/vnd.rn-realflash");
        mimeTypes.put("rgb", "image/x-rgb");
        mimeTypes.put("rm", "audio/x-pn-realaudio");
        mimeTypes.put("rmi", "audio/mid");
        mimeTypes.put("rmm", "audio/x-pn-realaudio");
        mimeTypes.put("rmp", "audio/x-pn-realaudio-plugin");
        mimeTypes.put("rng", "application/ringing-tones");
        mimeTypes.put("rnx", "application/vnd.rn-realplayer");
        mimeTypes.put("roff", "application/x-troff");
        mimeTypes.put("rp", "image/vnd.rn-realpix");
        mimeTypes.put("rpm", "audio/x-pn-realaudio-plugin");
        mimeTypes.put("rt", "text/richtext");
        mimeTypes.put("rtf", "application/rtf");
        mimeTypes.put("rtx", "application/rtf");
        mimeTypes.put("rv", "video/vnd.rn-realvideo");
        mimeTypes.put("s", "text/x-asm");
        mimeTypes.put("s3m", "audio/s3m");
        mimeTypes.put("sbk", "application/x-tbook");
        mimeTypes.put("scm", "video/x-scm");
        mimeTypes.put("sdml", "text/plain");
        mimeTypes.put("sdp", "application/sdp");
        mimeTypes.put("sdr", "application/sounder");
        mimeTypes.put("sea", "application/sea");
        mimeTypes.put("set", "application/set");
        mimeTypes.put("sgm", "text/sgml");
        mimeTypes.put("sgml", "text/sgml");
        mimeTypes.put("sh", "text/x-script.sh");
        mimeTypes.put("shar", "application/x-shar");
        mimeTypes.put("shtml", "text/html");
        mimeTypes.put("sid", "audio/x-psid");
        mimeTypes.put("sit", "application/x-sit");
        mimeTypes.put("skd", "application/x-koan");
        mimeTypes.put("skm", "application/x-koan");
        mimeTypes.put("skp", "application/x-koan");
        mimeTypes.put("skt", "application/x-koan");
        mimeTypes.put("sl", "application/x-seelogo");
        mimeTypes.put("smi", "application/smil");
        mimeTypes.put("smil", "application/smil");
        mimeTypes.put("snd", "audio/basic");
        mimeTypes.put("sol", "application/solids");
        mimeTypes.put("spc", "text/x-speech");
        mimeTypes.put("spl", "application/futuresplash");
        mimeTypes.put("spr", "application/x-sprite");
        mimeTypes.put("sprite", "application/x-sprite");
        mimeTypes.put("src", "application/x-wais-source");
        mimeTypes.put("ssi", "text/x-server-parsed-html");
        mimeTypes.put("ssm", "application/streamingmedia");
        mimeTypes.put("sst", "application/vnd.ms-pki.certstore");
        mimeTypes.put("step", "application/step");
        mimeTypes.put("stl", "application/sla");
        mimeTypes.put("stp", "application/step");
        mimeTypes.put("sv4cpio", "application/x-sv4cpio");
        mimeTypes.put("sv4crc", "application/x-sv4crc");
        mimeTypes.put("svg", "image/svg+xml");
        mimeTypes.put("svf", "image/x-dwg");
        mimeTypes.put("svr", "application/x-world");
        mimeTypes.put("swf", "application/x-shockwave-flash");
        mimeTypes.put("t", "application/x-troff");
        mimeTypes.put("talk", "text/x-speech");
        mimeTypes.put("tar", "application/x-tar");
        mimeTypes.put("tbk", "application/toolbook");
        mimeTypes.put("tcl", "application/x-tcl");
        mimeTypes.put("tcsh", "text/x-script.tcsh");
        mimeTypes.put("tex", "application/x-tex");
        mimeTypes.put("texi", "application/x-texinfo");
        mimeTypes.put("texinfo", "application/x-texinfo");
        mimeTypes.put("text", "text/plain");
        mimeTypes.put("tgz", "application/gnutar");
        mimeTypes.put("tif", "image/tiff");
        mimeTypes.put("tiff", "image/tiff");
        mimeTypes.put("tr", "application/x-troff");
        mimeTypes.put("tsi", "audio/tsp-audio");
        mimeTypes.put("tsp", "application/dsptype");
        mimeTypes.put("tsv", "text/tab-separated-values");
        mimeTypes.put("turbot", "image/florian");
        mimeTypes.put("txt", "text/plain");
        mimeTypes.put("uil", "text/x-uil");
        mimeTypes.put("uni", "text/uri-list");
        mimeTypes.put("unis", "text/uri-list");
        mimeTypes.put("unv", "application/i-deas");
        mimeTypes.put("uri", "text/uri-list");
        mimeTypes.put("uris", "text/uri-list");
        mimeTypes.put("ustar", "application/x-ustar");
        mimeTypes.put("uu", "text/x-uuencode");
        mimeTypes.put("uue", "text/x-uuencode");
        mimeTypes.put("vcd", "application/x-cdlink");
        mimeTypes.put("vcs", "text/x-vcalendar");
        mimeTypes.put("vda", "application/vda");
        mimeTypes.put("vdo", "video/vdo");
        mimeTypes.put("vew", "application/groupwise");
        mimeTypes.put("viv", "video/vivo");
        mimeTypes.put("vivo", "video/vivo");
        mimeTypes.put("vmd", "application/vocaltec-media-desc");
        mimeTypes.put("vmf", "application/vocaltec-media-file");
        mimeTypes.put("voc", "audio/voc");
        mimeTypes.put("vos", "video/vosaic");
        mimeTypes.put("vox", "audio/voxware");
        mimeTypes.put("vqe", "audio/x-twinvq-plugin");
        mimeTypes.put("vqf", "audio/x-twinvq");
        mimeTypes.put("vql", "audio/x-twinvq-plugin");
        mimeTypes.put("vrml", "model/vrml");
        mimeTypes.put("vrt", "x-world/x-vrt");
        mimeTypes.put("vsd", "application/x-visio");
        mimeTypes.put("vst", "application/x-visio");
        mimeTypes.put("vsw", "application/x-visio");
        mimeTypes.put("w60", "application/wordperfect6.0");
        mimeTypes.put("w61", "application/wordperfect6.1");
        mimeTypes.put("w6w", "application/msword");
        mimeTypes.put("wav", "audio/wav");
        mimeTypes.put("wb1", "application/x-qpro");
        mimeTypes.put("wbmp", "image/vnd.wap.wbmp");
        mimeTypes.put("web", "application/vnd.xara");
        mimeTypes.put("wiz", "application/msword");
        mimeTypes.put("wk1", "application/x-123");
        mimeTypes.put("wmf", "windows/metafile");
        mimeTypes.put("wml", "text/vnd.wap.wml");
        mimeTypes.put("wmlc", "application/vnd.wap.wmlc");
        mimeTypes.put("wmls", "text/vnd.wap.wmlscript");
        mimeTypes.put("wmlsc", "application/vnd.wap.wmlscriptc");
        mimeTypes.put("woff", "font/woff");
        mimeTypes.put("woff2", "font/woff2");
        mimeTypes.put("word", "application/msword");
        mimeTypes.put("wp", "application/wordperfect");
        mimeTypes.put("wp5", "application/wordperfect");
        mimeTypes.put("wp6", "application/wordperfect");
        mimeTypes.put("wpd", "application/wordperfect");
        mimeTypes.put("wq1", "application/x-lotus");
        mimeTypes.put("wri", "application/mswrite");
        mimeTypes.put("wrl", "x-world/x-vrml");
        mimeTypes.put("wrz", "model/vrml");
        mimeTypes.put("wsc", "text/scriplet");
        mimeTypes.put("wsrc", "application/x-wais-source");
        mimeTypes.put("wtk", "application/x-wintalk");
        mimeTypes.put("xbm", "image/xbm");
        mimeTypes.put("xdr", "video/x-amt-demorun");
        mimeTypes.put("xgz", "xgl/drawing");
        mimeTypes.put("xif", "image/vnd.xiff");
        mimeTypes.put("xl", "application/excel");
        mimeTypes.put("xla", "application/excel");
        mimeTypes.put("xlb", "application/excel");
        mimeTypes.put("xlc", "application/excel");
        mimeTypes.put("xld", "application/excel");
        mimeTypes.put("xlk", "application/excel");
        mimeTypes.put("xll", "application/excel");
        mimeTypes.put("xlm", "application/excel");
        mimeTypes.put("xls", "application/excel");;
        mimeTypes.put("xlt", "application/excel");
        mimeTypes.put("xlv", "application/excel");
        mimeTypes.put("xlw", "application/excel");
        mimeTypes.put("xm", "audio/xm");
        mimeTypes.put("xml", "application/xml");
        mimeTypes.put("xmz", "xgl/movie");
        mimeTypes.put("xpix", "application/x-vnd.ls-xpix");
        mimeTypes.put("xpm", "image/xpm");
        mimeTypes.put("x-png", "image/png");
        mimeTypes.put("xsr", "video/x-amt-showrun");
        mimeTypes.put("xwd", "image/x-xwindowdump");
        mimeTypes.put("xyz", "chemical/x-pdb");
        mimeTypes.put("z", "application/x-compressed");
        mimeTypes.put("zip", "application/zip");
        mimeTypes.put("zsh", "text/x-script.zsh");
    }

    public static @NonNull String getMimeForType(@Nullable String type) {
        if (type == null) {
            return "application/octet-stream";
        } else {
            return mimeTypes.getOrDefault(type.toLowerCase(), "application/octet-stream");
        }
    }

    public static @NonNull String getMimeForFile(@NonNull File file) {
        String name[] = file.getName().split("\\.");

        if (name.length == 1) {
            return "application/octet-stream";
        } else {
            return getMimeForType(name[name.length - 1]);
        }
    }

}
