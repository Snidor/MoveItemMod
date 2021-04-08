package moveitemmod;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;

import com.wurmonline.client.WurmClientBase;
import com.wurmonline.client.game.World;
import com.wurmonline.client.renderer.SubPickableUnit;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;

import javassist.ClassPool;
import javassist.CtClass;

public class MoveItemMod implements WurmClientMod, Initable, PreInitable 
{
	public static Logger LOGGER = Logger.getLogger( "MoveItemMod" );
	public static HeadsUpDisplay mHud;
	public static World mWorld;
	public static boolean mShowIDs = false;
	public static boolean mHiddenInventory = false;
	public static long mTarget;
	
	public static boolean handleInput( final String pCommand, final String[] pData ) 
	{
		if ( pCommand.equalsIgnoreCase( "moveitem" ) ) 
		{
			if ( pData.length == 2 ) 
			{
				WurmClientBase lClient = mWorld.getClient();
				long[] lSource = mHud.getCommandTargetsFrom( lClient.getXMouse(), lClient.getYMouse() );
				mTarget = Long.parseLong( pData[1] );
				mHud.getInventoryWindow().getInventoryListComponent();

				if ( lSource != null )
				{
					mWorld.getServerConnection().sendMoveSomeItems( mTarget, lSource );
					mHud.consoleOutput( "Move items into " + mTarget );					
				}
				else
				{	
					List<SubPickableUnit> lList = mWorld.getCurrentHoveredObject().getSubPickableUnitList();
					for ( int i = 0; i < lList.size(); i ++ )
					{
						LOGGER.log(Level.INFO, "SubList: " + lList.get( i ).getId() );					
					}
					mHud.consoleOutput( "Couldn't move item, no target found" );	
				}
				return true;			
			}		
			return true;
		}
		
		else if ( pCommand.equalsIgnoreCase( "showids" ) )
		{
            if ( pData.length == 2) 
            {
                if ( pData[1].equals( "on" ) ) 
                {
                    mHud.consoleOutput( "Show IDs on" );
                    mShowIDs = true;
                    return true;
                } 
                else if ( pData[1].equals( "off" ) ) 
                {
                    mHud.consoleOutput( "Show IDs off" );
                    mShowIDs = false;
                    return true;
                }
            }
            mHud.consoleOutput( "Usage: showids {on|off}" );
            return true;
        } 		
		return false;
	}

	@Override
	public void preInit() 
	{
		LOGGER.log( Level.INFO, "Init MoveItemMod" );
		try 
		{
			ClassPool lClassPool = HookManager.getInstance().getClassPool();

	        CtClass lCtInventoryMetaItem = lClassPool.getCtClass("com.wurmonline.client.game.inventory.InventoryMetaItem");
	        lCtInventoryMetaItem.getMethod("getHoverText", "()Ljava/lang/String;").insertBefore("if ( moveitemmod.MoveItemMod.mShowIDs ) return this.hoverText + \" (\"+ this.id +\")\";");

	        CtClass lCtObjectData = lClassPool.getCtClass("com.wurmonline.client.renderer.ObjectData");
	        lCtObjectData.getMethod("getHoverText", "()Ljava/lang/String;").insertBefore("if ( moveitemmod.MoveItemMod.mShowIDs ) return this.hoverText.toString() + \" (\"+ this.id +\")\";");

	        CtClass lCtWurmConsole = lClassPool.getCtClass("com.wurmonline.client.console.WurmConsole");
	        lCtWurmConsole.getMethod("handleDevInput", "(Ljava/lang/String;[Ljava/lang/String;)Z").insertBefore("if (moveitemmod.MoveItemMod.handleInput($1,$2)) return true;");

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
			LOGGER.log( Level.SEVERE, "Error MoveItemMod", e.getMessage() );
		}
	}
	
	@Override
	public void init() 
	{
		
	}
}
