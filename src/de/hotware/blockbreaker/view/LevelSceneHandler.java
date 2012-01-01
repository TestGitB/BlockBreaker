package de.hotware.blockbreaker.view;

import java.util.Vector;

import org.andengine.engine.Engine;
import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.FadeInModifier;
import org.andengine.entity.modifier.FadeOutModifier;
import org.andengine.entity.modifier.IEntityModifier.IEntityModifierListener;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.shape.Shape;
import org.andengine.entity.sprite.TiledSprite;
import org.andengine.entity.text.ChangeableText;
import org.andengine.entity.text.Text;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.util.modifier.IModifier;

import de.hotware.blockbreaker.model.Block;
import de.hotware.blockbreaker.model.Block.BlockColor;
import de.hotware.blockbreaker.model.IGravityListener;
import de.hotware.blockbreaker.model.Level;
import de.hotware.blockbreaker.model.INextBlockListener;
import de.hotware.blockbreaker.model.WinCondition;

public class LevelSceneHandler {
	
	private static final int SPRITE_TEXTURE_HEIGHT = UIConstants.BASE_SPRITE_HEIGHT;
	private static final int SPRITE_TEXTURE_WIDTH = UIConstants.BASE_SPRITE_WIDTH;
	
	private static final int HORIZONTAL_SPARE = (UIConstants.LEVEL_WIDTH - (6 * SPRITE_TEXTURE_WIDTH)) - 7;
    private static final int HORIZONTAL_GAP =  HORIZONTAL_SPARE/2;
    private static final int HORIZONTAL_SIZE = UIConstants.LEVEL_WIDTH - HORIZONTAL_SPARE;
    private static final int VERTICAL_SPARE = UIConstants.LEVEL_HEIGHT - 6 * SPRITE_TEXTURE_HEIGHT - 7;
    private static final int VERTICAL_GAP =  VERTICAL_SPARE/2;
    private static final int VERTICAL_SIZE = UIConstants.LEVEL_HEIGHT - VERTICAL_SPARE;
	
    @SuppressWarnings("unused")
	private Engine mEngine;
	private Font mUIFont;
	private TiledTextureRegion mBlockTiledTextureRegion;
	private TiledTextureRegion mArrowTiledTextureRegion;
	private Scene mScene;
	private Level mLevel;
	private BlockSpritePool mBlockSpritePool;
	private TiledSprite mNextBlockSprite;
	private ChangeableText mTurnsLeftText;
	private ChangeableText[] mWinCondText;
	
	private IBlockSpriteTouchListener mBlockSpriteTouchListener;
	private INextBlockListener mNextBlockListener;
	private IGravityListener mGravityListener;
	
	private Vector<BlockSprite> mBlockSpriteList;
	
	public LevelSceneHandler(Engine pEngine,
			Font pUIFont,
			TiledTextureRegion pBlockTiledTextureRegion,
			TiledTextureRegion pArrowTiledTextureRegion) {
		this.mEngine = pEngine;
		this.mUIFont = pUIFont;
		this.mBlockTiledTextureRegion = pBlockTiledTextureRegion;
		this.mArrowTiledTextureRegion = pArrowTiledTextureRegion;		
		this.mWinCondText = new ChangeableText[5];
		this.mBlockSpriteList = new Vector<BlockSprite>();
	}
	
	public Scene createLevelScene(final Level pLevel) {
		
		this.mLevel = pLevel;
		
		//TODO make to: LevelSceneHandler mit reset Funktionalität, der nur das level killt und alles neu einstellt,
		// wenn es gebraucht wird. LevelSceneHandler soll als einziger an Scene rumspielen (außer Hintergrund)
	    this.mScene = new Scene();
	    this.mBlockSpritePool = new BlockSpritePool(this.mScene, this.mBlockTiledTextureRegion);
        
        //create surroundings
        final Shape ground = new Rectangle(HORIZONTAL_GAP - 1, UIConstants.LEVEL_HEIGHT-VERTICAL_GAP + 1, HORIZONTAL_SIZE + 3, 1);
        final Shape roof = new Rectangle(HORIZONTAL_GAP - 1, VERTICAL_GAP - 1, HORIZONTAL_SIZE + 3, 1);
        final Shape left = new Rectangle(HORIZONTAL_GAP-1, VERTICAL_GAP - 1, 1, VERTICAL_SIZE + 4);
        final Shape right = new Rectangle(UIConstants.LEVEL_WIDTH - HORIZONTAL_GAP + 1, VERTICAL_GAP -1, 1, VERTICAL_SIZE + 4);

        this.mScene.attachChild(ground);
        this.mScene.attachChild(roof);
        this.mScene.attachChild(left);
        this.mScene.attachChild(right);
        //create surroundings end
        
        //init BlockTouchListener
        this.mBlockSpriteTouchListener = new BasicBlockSpriteTouchListener();
        //init BlockTouchListener end
        
        //init playfield
        this.initPlayField();
        //init playfield end
        
        //init UI
        TiledSprite winSpriteHelp;
        for(int i = 0; i < 5; ++i) {
        	winSpriteHelp = new TiledSprite(
    	    		5,
    	    		17 + VERTICAL_GAP + (SPRITE_TEXTURE_HEIGHT+5)*i,
    	    		SPRITE_TEXTURE_WIDTH,
    	    		SPRITE_TEXTURE_HEIGHT,
    	    		this.mBlockTiledTextureRegion.deepCopy());
    	    winSpriteHelp.setCurrentTileIndex(i+1);
    	    mScene.attachChild(winSpriteHelp);
        }
        
		final WinCondition winCondition = pLevel.getWinCondition();
        ChangeableText winDisplayText;
        for(int i = 1; i < 6; ++i) {
        	winDisplayText = new ChangeableText(
    	    		10 + SPRITE_TEXTURE_WIDTH,
    	    		30 + VERTICAL_GAP + (SPRITE_TEXTURE_HEIGHT+5)*(i-1),
    	    		this.mUIFont,
    	    		Integer.toString(winCondition.getWinCount(i)), 1);
        	this.mWinCondText[i-1] = winDisplayText;
    	    mScene.attachChild(winDisplayText);
        }
        
        final Text nextText = new Text(0, 0, this.mUIFont, "Next");
	    nextText.setPosition(
	    		UIConstants.LEVEL_WIDTH - nextText.getWidth() - 13,
	    		2 + VERTICAL_GAP);
	    mScene.attachChild(nextText);
	    
	    this.mNextBlockSprite = new TiledSprite(
	    		UIConstants.LEVEL_WIDTH - SPRITE_TEXTURE_WIDTH - 24,
	    		nextText.getY() + nextText.getHeight() + 10,
	    		SPRITE_TEXTURE_WIDTH,
	    		SPRITE_TEXTURE_HEIGHT,
	    		this.mBlockTiledTextureRegion.deepCopy());
	    this.mNextBlockSprite.setCurrentTileIndex(pLevel.getNextBlock().getColor().toNumber());
	    mScene.attachChild(this.mNextBlockSprite);
	    
	    final Text turnsText = new Text(0, 0, this.mUIFont, "Turns");
	    turnsText.setPosition(
	    		UIConstants.LEVEL_WIDTH - turnsText.getWidth() - 2,
	    		this.mNextBlockSprite.getY() + this.mNextBlockSprite.getHeight() + 10);
	    mScene.attachChild(turnsText);
	    
	    this.mTurnsLeftText = new ChangeableText(0, 0, this.mUIFont, pLevel.getBlocksDisplayText() , 3);
	    this.mTurnsLeftText.setPosition(
	    		UIConstants.LEVEL_WIDTH - this.mTurnsLeftText.getWidth() - 22,
	    		turnsText.getY() + turnsText.getHeight() + 10);
	    mScene.attachChild(this.mTurnsLeftText);
	    
	    final TiledSprite nextBlockSprite = this.mNextBlockSprite;
	    final ChangeableText turnsLeftText = this.mTurnsLeftText;
	    pLevel.setNextBlockListener(this.mNextBlockListener = new INextBlockListener() {	
	    	
			@Override
			public void onNextBlockChanged(NextBlockChangedEvent pEvt) {
				nextBlockSprite.setCurrentTileIndex(pEvt.getNextBlock().getColor().toNumber());
				turnsLeftText.setText(pEvt.getSource().getBlocksDisplayText());
			}	
			
	    });
	    
	    final TiledSprite gravityArrowSprite = new TiledSprite(
	    		UIConstants.LEVEL_WIDTH - SPRITE_TEXTURE_WIDTH - 24,
	    		turnsLeftText.getY() + turnsLeftText.getHeight() + 10,
	    		SPRITE_TEXTURE_WIDTH,
	    		SPRITE_TEXTURE_HEIGHT,
	    		this.mArrowTiledTextureRegion.deepCopy());
	    gravityArrowSprite.setCurrentTileIndex(pLevel.getGravity().toNumber());
	    mScene.attachChild(gravityArrowSprite);
	    
	    pLevel.setGravityListener(this.mGravityListener = new IGravityListener() {
			@Override
			public void onGravityChanged(GravityEvent pEvt) {
				gravityArrowSprite.setCurrentTileIndex(pLevel.getGravity().toNumber());
			}	    	
	    });	 
	    //init UI end  
	    return mScene;
	}
	
	private void initPlayField() {
		Block[][] matrix = this.mLevel.getMatrix();
        for(int i = 0; i < 6; ++i) {
        	for(int j = 0; j < 6; ++j) {
	        	addBlockSprite(matrix[i][j]);
        	}
	    }
	}
	
	public void updateLevel(Level pLevel) {
		this.resetScene();
		this.mLevel = pLevel;
		this.initPlayField();
		pLevel.setGravityListener(this.mGravityListener);
		pLevel.setNextBlockListener(this.mNextBlockListener);
		for(int i = 1; i < 6; ++i) {
				this.mWinCondText[i-1].setText(Integer.toString(pLevel.getWinCondition().getWinCount(i)));
		}
		this.mTurnsLeftText.setText(pLevel.getBlocksDisplayText());
		this.mNextBlockSprite.setCurrentTileIndex(pLevel.getNextBlock().getColor().toNumber());
		
	}
	
	private void resetScene() {
		for(BlockSprite bs : this.mBlockSpriteList) {
			this.mBlockSpritePool.recyclePoolItem(bs);
		}
		this.mBlockSpriteList.clear();
	}
	
	private BlockSprite addBlockSprite(final Block pBlock) {
		int x = pBlock.getX();
		int y = pBlock.getY();
		BlockSprite sprite = this.mBlockSpritePool.obtainBlockSprite(2 + HORIZONTAL_GAP + x * (SPRITE_TEXTURE_WIDTH + 1),
				2 + VERTICAL_GAP + y * (SPRITE_TEXTURE_HEIGHT + 1),
				pBlock, 
				this.mBlockSpriteTouchListener);
		this.mBlockSpriteList.add(sprite);
        sprite.setCurrentTileIndex(pBlock.getColor().toNumber());
        pBlock.setBlockPositionListener(new BasicBlockPositionListener(sprite));
        this.mScene.registerTouchArea(sprite);
        return sprite;
	}
	
	private class BasicBlockSpriteTouchListener implements IBlockSpriteTouchListener{
		
		@Override
		public void onBlockSpriteTouch(BlockSpriteTouchEvent pEvt) {
			
			Block oldBlock = pEvt.getBlock();
            int x = oldBlock.getX();
            int y = oldBlock.getY();
            
            Block block = LevelSceneHandler.this.mLevel.killBlock(x, y);
            
            if(block.getColor() != BlockColor.NONE) {
            	pEvt.getSource().registerEntityModifier(new FadeOutModifier(UIConstants.SPRITE_FADE_OUT_TIME, new IEntityModifierListener() {
            		
					@Override
					public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
						BlockSprite bs = (BlockSprite) pItem;
						bs.setCurrentTileIndex(0);
						LevelSceneHandler.this.mScene.unregisterTouchArea(bs);
					}
					
					@Override
					public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
						LevelSceneHandler.this.mBlockSpriteList.remove(pItem);
						LevelSceneHandler.this.mBlockSpritePool.recyclePoolItem((BlockSprite) pItem);
					}
		        	
		        }));
	            
	            addBlockSprite(block)
	            	.registerEntityModifier(new FadeInModifier(UIConstants.SPRITE_FADE_IN_TIME));
            }	            	
        }		
	}
}
