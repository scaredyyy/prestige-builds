package dev.zprestige.prestige.client.util.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.zprestige.prestige.client.shader.impl.GradientGlowShader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.awt.Color;
import java.util.ArrayList;

/** Minecraft 1.21 immediate-mode rendering helpers. */
public final class RenderUtil {
    public static GradientGlowShader shader;
    private RenderUtil() {}
    private static MatrixStack ms() { return RenderHelper.getMatrixStack() == null ? new MatrixStack() : RenderHelper.getMatrixStack(); }
    private static BufferBuilder begin(VertexFormat.DrawMode mode, VertexFormat format) { return Tessellator.getInstance().begin(mode, format); }
    private static void draw(BufferBuilder buffer) { BufferRenderer.drawWithGlobalProgram(buffer.end()); }
    private static float r(Color c) { return c.getRed() / 255F; } private static float g(Color c) { return c.getGreen() / 255F; }
    private static float b(Color c) { return c.getBlue() / 255F; } private static float a(Color c) { return c.getAlpha() / 255F; }
    private static void v(BufferBuilder b, Matrix4f m, float x, float y, float z, Color c) { b.vertex(m,x,y,z).color(r(c),g(c),b(c),a(c)); }

    public static void renderItem(ItemStack s, float x, float y, float scale, boolean overlay) {
        if (RenderHelper.getContext() == null) return; MatrixStack m=ms(); m.push(); m.scale(scale,scale,1); RenderHelper.getContext().drawItem(s,(int)(x/scale),(int)(y/scale));
        if (overlay) RenderHelper.getContext().drawItemInSlot(MinecraftClient.getInstance().textRenderer,s,(int)(x/scale),(int)(y/scale)); m.pop();
    }
    public static void renderCircleOutline(float x,float y,float radius,Color c) { circle(x,y,radius,c,true); }
    public static void renderFilledCircle(float x,float y,float radius,Color c) { circle(x,y,radius,c,false); }
    private static void circle(float x,float y,float radius,Color c,boolean line) {
        Matrix4f m=ms().peek().getPositionMatrix(); RenderSystem.enableBlend(); RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder q=begin(line?VertexFormat.DrawMode.DEBUG_LINE_STRIP:VertexFormat.DrawMode.TRIANGLE_FAN,VertexFormats.POSITION_COLOR);
        if(!line)v(q,m,x,y,0,c); for(int i=0;i<=64;i++){double d=Math.PI*2*i/64D;v(q,m,x+(float)Math.cos(d)*radius,y+(float)Math.sin(d)*radius,0,c);} draw(q); RenderSystem.disableBlend();
    }
    public static void renderTexturedRect(float x,float y,float w,float h,Identifier id,Color c) { RenderSystem.setShaderColor(r(c),g(c),b(c),a(c)); renderTexturedQuad(x,y,w,h,id); RenderSystem.setShaderColor(1,1,1,1); }
    public static void renderTexturedQuad(float x,float y,float w,float h,Identifier id) {
        Matrix4f m=ms().peek().getPositionMatrix(); RenderSystem.enableBlend(); RenderSystem.setShader(GameRenderer::getPositionTexProgram); RenderSystem.setShaderTexture(0,id);
        BufferBuilder q=begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION_TEXTURE); q.vertex(m,x,y,0).texture(0,0); q.vertex(m,x,y+h,0).texture(0,1); q.vertex(m,x+w,y+h,0).texture(1,1); q.vertex(m,x+w,y,0).texture(1,0); draw(q); RenderSystem.disableBlend();
    }
    public static void renderTexturedQuad(Identifier id,float x,float y,float z,float w,float h,int rw,int rh,int tw,int th) { RenderSystem.setShaderTexture(0,id); renderTexturedQuad(x,x+w,y,y+h,z,(int)w,(int)h,0,0,tw,th); }
    public static void renderTexturedQuad(float x1,float x2,float y1,float y2,float z,int w,int h,float u,float vv,int tw,int th) {
        Matrix4f m=ms().peek().getPositionMatrix(); RenderSystem.setShader(GameRenderer::getPositionTexProgram); BufferBuilder q=begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION_TEXTURE);
        q.vertex(m,x1,y2,z).texture(u/tw,(vv+h)/th);q.vertex(m,x2,y2,z).texture((u+w)/tw,(vv+h)/th);q.vertex(m,x2,y1,z).texture((u+w)/tw,vv/th);q.vertex(m,x1,y1,z).texture(u/tw,vv/th);draw(q);
    }
    public static void renderColoredQuad(float x1,float y1,float x2,float y2,Color c) { renderColoredQuad(x1,y1,x2,y2,c,c,c,c); }
    public static void renderColoredQuad(float x1,float y1,float x2,float y2,boolean tl,boolean tr,boolean bl,boolean br,float alpha) {
        Color z=new Color(0,0,0,0), q=new Color(0,0,0,MathHelper.clamp((int)alpha,0,255)); renderColoredQuad(x1,y1,x2,y2,tl?q:z,tr?q:z,bl?q:z,br?q:z);
    }
    public static void renderColoredQuad(float x1,float y1,float x2,float y2,Color tl,Color tr,Color bl,Color br) {
        Matrix4f m=ms().peek().getPositionMatrix(); RenderSystem.enableBlend(); RenderSystem.setShader(GameRenderer::getPositionColorProgram); BufferBuilder q=begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION_COLOR);
        v(q,m,x1,y1,0,tl);v(q,m,x1,y2,0,bl);v(q,m,x2,y2,0,br);v(q,m,x2,y1,0,tr);draw(q);RenderSystem.disableBlend();
    }
    public static void renderRoundedRectOutline(float x1,float y1,float x2,float y2,Color c,float radius) { rounded(x1,y1,x2,y2,c,radius,true); }
    public static void renderRoundedRect(float x1,float y1,float x2,float y2,Color c,float radius) { rounded(x1,y1,x2,y2,c,radius,false); }
    private static void rounded(float x1,float y1,float x2,float y2,Color c,float radius,boolean outline) {
        Matrix4f m=ms().peek().getPositionMatrix(); RenderSystem.enableBlend();RenderSystem.setShader(GameRenderer::getPositionColorProgram);BufferBuilder q=begin(outline?VertexFormat.DrawMode.DEBUG_LINE_STRIP:VertexFormat.DrawMode.TRIANGLE_FAN,VertexFormats.POSITION_COLOR);
        if(!outline)v(q,m,(x1+x2)/2,(y1+y2)/2,0,c); for(int i=0;i<=64;i++){double d=Math.PI*2*i/64D;float x=(float)((x1+x2)/2+(x2-x1)/2*Math.cos(d));float y=(float)((y1+y2)/2+(y2-y1)/2*Math.sin(d));v(q,m,x,y,0,c);}draw(q);RenderSystem.disableBlend();
    }
    public static void renderColoredRoundedRect(float x1,float y1,float x2,float y2,float rad,Color tl,Color tr,Color bl,Color br) { renderColoredQuad(x1,y1,x2,y2,tl,tr,bl,br); }
    public static void renderGradient(float x1,float y1,float x2,float y2,Color c,float alpha) { renderColoredQuad(x1,y1,x2,y2,new Color(c.getRed(),c.getGreen(),c.getBlue(),0),c,c,new Color(c.getRed(),c.getGreen(),c.getBlue(),0)); }
    public static void renderCircularGradient(float x1,float y1,float x2,float y2,Color c,float rad) { renderRoundedRect(x1,y1,x2,y2,c,rad); }
    public static void renderCrossed(float x1,float y1,float x2,float y2,Color c) {
        Matrix4f m=ms().peek().getPositionMatrix();RenderSystem.setShader(GameRenderer::getPositionColorProgram);BufferBuilder q=begin(VertexFormat.DrawMode.DEBUG_LINES,VertexFormats.POSITION_COLOR);v(q,m,x1,y1,0,c);v(q,m,x2,y2,0,c);v(q,m,x2,y1,0,c);v(q,m,x1,y2,0,c);draw(q);
    }
    public static void renderColoredEllipseBorder(float x1,float y1,float x2,float y2,Color c,float rad){renderRoundedRectOutline(x1,y1,x2,y2,c,rad);}
    public static void renderColoredRectangleOutline(float x1,float y1,float x2,float y2,Color c){renderRoundedRectOutline(x1,y1,x2,y2,c,0);}
    public static void renderArrows(float x,float y,float size,Color c){renderCrossed(x-size,y,x+size,y,c);}
    public static void renderShaderRect(MatrixStack m,Color c1,Color c2,Color c3,Color c4,float x,float y,float w,float h,float radius,float softness){renderColoredQuad(x,y,x+w,y+h,c1,c2,c3,c4);}
    public static void renderFilledBox(float x1,float y1,float z1,float x2,float y2,float z2,Color c) {
        Matrix4f m=ms().peek().getPositionMatrix();RenderSystem.enableBlend();RenderSystem.disableDepthTest();RenderSystem.setShader(GameRenderer::getPositionColorProgram);BufferBuilder q=begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION_COLOR);
        float[][] p={{x1,y1,z1},{x2,y1,z1},{x2,y2,z1},{x1,y2,z1},{x1,y1,z2},{x2,y1,z2},{x2,y2,z2},{x1,y2,z2}};int[][] f={{0,1,2,3},{5,4,7,6},{4,0,3,7},{1,5,6,2},{3,2,6,7},{4,5,1,0}};for(int[] face:f)for(int i:face)v(q,m,p[i][0],p[i][1],p[i][2],c);draw(q);RenderSystem.enableDepthTest();RenderSystem.disableBlend();
    }
    public static void renderOutlinedBox(float x1,float y1,float z1,float x2,float y2,float z2,Color c) {
        Matrix4f m=ms().peek().getPositionMatrix();RenderSystem.setShader(GameRenderer::getPositionColorProgram);BufferBuilder q=begin(VertexFormat.DrawMode.DEBUG_LINES,VertexFormats.POSITION_COLOR);float[][] p={{x1,y1,z1},{x2,y1,z1},{x2,y2,z1},{x1,y2,z1},{x1,y1,z2},{x2,y1,z2},{x2,y2,z2},{x1,y2,z2}};int[][] e={{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};for(int[] edge:e)for(int i:edge)v(q,m,p[i][0],p[i][1],p[i][2],c);draw(q);
    }
    public static void renderColoredEllipse3D(float x,float y,float z,float rad,Color c){Matrix4f m=ms().peek().getPositionMatrix();RenderSystem.setShader(GameRenderer::getPositionColorProgram);BufferBuilder q=begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,VertexFormats.POSITION_COLOR);for(int i=0;i<=64;i++){double d=Math.PI*2*i/64D;v(q,m,x+(float)Math.cos(d)*rad,y,z+(float)Math.sin(d)*rad,c);}draw(q);}
    public static void renderLines(ArrayList<Vec3d> points,Color c){if(points.size()<2)return;Matrix4f m=ms().peek().getPositionMatrix();RenderSystem.setShader(GameRenderer::getPositionColorProgram);BufferBuilder q=begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,VertexFormats.POSITION_COLOR);for(Vec3d p:points)v(q,m,(float)p.x,(float)p.y,(float)p.z,c);draw(q);}
    public static void setScissorRegion(float x1,float y1,float x2,float y2){
        DrawContext context=RenderHelper.getContext();
        if(context!=null) context.enableScissor((int)x1,(int)y1,(int)x2,(int)y2);
    }
    public static void clearScissor(){
        DrawContext context=RenderHelper.getContext();
        if(context!=null) context.disableScissor();
    }
    public static void setCameraAction(){Camera c=MinecraftClient.getInstance().getBlockEntityRenderDispatcher().camera;if(c==null)return;MatrixStack m=ms();m.push();Vec3d p=c.getPos();m.translate(-p.x,-p.y,-p.z);}
    public static Color getThemeColor(Color c,int index,int total){float[] h=Color.RGBtoHSB(c.getRed(),c.getGreen(),c.getBlue(),null);h[2]=.25F+.75F*(float)Math.abs(((System.currentTimeMillis()%2000L)/1000D+(double)index/total*2)%2-1);return new Color(Color.HSBtoRGB(h[0],h[1],h[2]));}
    public static Color getColor(int n,float alpha){return new Color((14+n)/255F,(14+n)/255F,(14+n)/255F,MathHelper.clamp(alpha,0,1));}
    public static Color getColor(Color c,float alpha){return new Color(r(c),g(c),b(c),MathHelper.clamp(alpha,0,1));}
    public static Color getColor(float value,float alpha){return new Color(value,value,value,MathHelper.clamp(alpha,0,1));}
    public static Vec3d worldSpaceToScreenSpace(Vec3d pos){
        MinecraftClient mc=MinecraftClient.getInstance();Camera camera=mc.getEntityRenderDispatcher().camera;int[] viewport={0,0,mc.getWindow().getFramebufferWidth(),mc.getWindow().getFramebufferHeight()};Vector3f target=new Vector3f();Vector4f point=new Vector4f((float)(pos.x-camera.getPos().x),(float)(pos.y-camera.getPos().y),(float)(pos.z-camera.getPos().z),1).mul(RenderHelper.getPositionMatrix());Matrix4f projection=new Matrix4f(RenderHelper.getProjectionMatrix()).mul(new Matrix4f(RenderHelper.getModelViewMatrix()));projection.project(point.x(),point.y(),point.z(),viewport,target);return new Vec3d(target.x/mc.getWindow().getScaleFactor(),(mc.getWindow().getHeight()-target.y)/mc.getWindow().getScaleFactor(),target.z);
    }
    public static Vec3d getEntityPos(Entity e){float t=MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true);return new Vec3d(MathHelper.lerp(t,e.prevX,e.getX()),MathHelper.lerp(t,e.prevY,e.getY()),MathHelper.lerp(t,e.prevZ,e.getZ()));}
}
