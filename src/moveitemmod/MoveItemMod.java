package moveitemmod;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;

import com.wurmonline.client.WurmClientBase;
import com.wurmonline.client.game.World;
import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;

import javassist.ClassPool;
import javassist.CtClass;

public class MoveItemMod implements WurmClientMod, Initable
{
	public static Logger LOGGER = Logger.getLogger("MoveItemMod");
	public static HeadsUpDisplay mHud;
	public static World mWorld;
	
	public static boolean handleInput( final String pCommand, final String[] data ) 
	{
		if ( pCommand.toLowerCase().equals("moveitem") ) 
		{
			WurmClientBase lClient = mWorld.getClient();
			InventoryMetaItem tTarget = mHud.getToolBelt().getItemInSlot(0);
			long[] lSource = mHud.getCommandTargetsFrom(lClient.getXMouse(), lClient.getYMouse());
			
			mWorld.getServerConnection().sendMoveSomeItems(tTarget.getId(), lSource);
			return true;
		}
		return false;
	}

	@Override
	public void init() 
	{
		LOGGER.log( Level.INFO, "Init MoveItemMod" );
		try 
		{
			ClassPool lClassPool = HookManager.getInstance().getClassPool();

			CtClass lCtWurmConsole = lClassPool.getCtClass( "com.wurmonline.client.console.WurmConsole" );
			lCtWurmConsole.getMethod( "handleDevInput", "(Ljava/lang/String;[Ljava/lang/String;)Z" ).insertBefore("if (moveitemmod.MoveItemMod.handleInput($1,$2)) return true;");
			
			HookManager.getInstance().registerHook( "com.wurmonline.client.renderer.gui.HeadsUpDisplay", "init", "(II)V", () -> ( pProxy, pMethod, pArgs ) -> 
			{
				pMethod.invoke( pProxy, pArgs );
				mHud = ( HeadsUpDisplay ) pProxy;
				return null;
			});
			
			HookManager.getInstance().registerHook( "com.wurmonline.client.renderer.WorldRender", "renderPickedItem", "(Lcom/wurmonline/client/renderer/backend/Queue;)V", () -> ( pProxy, pMethod, pArgs ) -> 
			{
				pMethod.invoke(pProxy, pArgs);
				Class<?> lCls = pProxy.getClass();

				mWorld = ReflectionUtil.getPrivateField( pProxy, ReflectionUtil.getField( lCls, "world" ) );

				return null;
			});
		} 
		catch ( Throwable e ) 
		{
			LOGGER.log( Level.SEVERE, "Error FreecamMod", e.getMessage() );
		}
	}
}
