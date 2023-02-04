<script type="text/javascript">
    if ( typeof( globalseed ) == "undefined") {
        var globalseed  = Math.round(Math.random()*65535);
    }

    function CAWBrowser(){
        var haveFlash = new Array();
        var i;
        for (i=6; i<15; i++ ) haveFlash[i] = false;
        var ua = navigator.userAgent;
        this.msie = (ua && ( parseFloat( navigator.appVersion )  >=4 ) && ( ua.indexOf("Opera") < 0 ) && ( ua.indexOf("MSIE 4") < 0 ) && ( ua.indexOf( "MSIE" ) >=0) );
        this.win = (ua && ((ua.indexOf( "Windows 95" ) >=0) || (ua.indexOf("Windows NT") >=0 ) || (ua.indexOf("Windows 98") >=0) ) );
        this.mac = (navigator.platform && (navigator.platform.indexOf('Mac')!=-1));
        this.opera7 = ((ua.indexOf('Opera') != -1) && window.opera && document.readyState) ? 1 : 0;
        this.gecko   = (ua.toLowerCase().indexOf('gecko') != -1) && (ua.indexOf('safari') == -1);

        var flash_nonie = (navigator.mimeTypes && navigator.mimeTypes["application/x-shockwave-flash"]) ? navigator.mimeTypes["application/x-shockwave-flash"].enabledPlugin : 0;

        if( flash_nonie){
            for (i=6; i<15; i++ ){
                haveFlash[i] = flash_nonie;
                haveFlash[i] = (parseInt(haveFlash[i].description.substring(haveFlash[i].description.indexOf(".")-2))>=i);
            }
        }else if ( this.msie && this.win && !this.mac){
            for (i=6; i<15; i++ )
                try{ haveFlash[i]  = new ActiveXObject("ShockwaveFlash.ShockwaveFlash." + i); }catch(e){};
        }

        this.other = !( (this.gecko || this.msie) && this.win && !this.mac);

        this.flash = 0;
        for (i=6; i<15; i++ ) if (haveFlash[i]) this.flash = i;
    }

    var aw_br = new CAWBrowser();
    var aw_http_proto = ("https:" == document.location.protocol ? "https:" : "http:");
    var jssrc = aw_http_proto + "//awaps.yandex.ru/8/20767/240200.?" + globalseed +
        "-0-" + globalseed  + "&swfcode=6&awcode=41&subsection=null&flash=" + aw_br.flash;

    var jssrc_code = '<sc'+'ript src="' + jssrc +'"></sc'+'ript>';

    document.write(jssrc_code);
</script>