#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTexCoord;
uniform samplerExternalOES sTexture;
uniform int uChoose;

void main() {
    if(uChoose == 0){
        gl_FragColor = texture2D(sTexture,vTexCoord);
    }
    else{
      vec2 uv = vTexCoord;
      if (vTexCoord.x>0.5){
        uv.x = 1.0 - vTexCoord.x;
      }
      gl_FragColor = texture2D(sTexture, uv);
    }
}