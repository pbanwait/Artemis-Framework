package com.artemis.systems;

import java.util.BitSet;

import com.artemis.Aspect;
import com.artemis.Entity;
import com.artemis.EntitySystem;
import com.artemis.utils.Bag;
import com.artemis.utils.ImmutableBag;

/**
 * The most raw entity system. It should not typically be used, but you can create your own
 * entity system handling by extending this. It is recommended that you use the other provided
 * entity system implementations.
 * 
 * @author MrParo
 *
 */
public abstract class RawSystem extends EntitySystem
{
	private Bag<Entity> actives;

	private BitSet allSet;
	private BitSet exclusionSet;
	private BitSet oneSet;

	private boolean dummy;
	
	/**
	 * Creates an entity system that uses the specified aspect as a matcher against entities.
	 * @param aspect to match against entities
	 */
	public RawSystem(Aspect aspect) {
		actives = new Bag<Entity>();
		allSet = aspect.getAllSet();
		exclusionSet = aspect.getExclusionSet();
		oneSet = aspect.getOneSet();
		dummy = allSet.isEmpty() && oneSet.isEmpty(); // This system can't possibly be interested in any entity, so it must be "dummy"
	}
	

	@Override
        protected void begin() {
	}

	@Override
        public final void process() {
		if(checkProcessing()) {
			begin();
			processEntities(actives);
			end();
		}
	}
	

	@Override
        protected void end() {
	}
	
	/**
	 * Any implementing entity system must implement this method and the logic
	 * to process the given entities of the system.
	 * 
	 * @param entities the entities this system contains.
	 */
	protected abstract void processEntities(ImmutableBag<Entity> entities);
	
	/**
	 * 
	 * @return true if the system should be processed, false if not.
	 */
	protected abstract boolean checkProcessing();


	@Override
        protected void initialize() {};

	/**
	 * Called if the system has received a entity it is interested in, e.g. created or a component was added to it.
	 * @param e the entity that was added to this system.
	 */
	protected void inserted(Entity e) {};

	/**
	 * Called if a entity was removed from this system, e.g. deleted or had one of it's components removed.
	 * @param e the entity that was removed from this system.
	 */
	protected void removed(Entity e) {};


	@Override
        protected final void check(Entity e) {
		if(dummy) {
			return;
		}
		
		boolean contains = e.getSystemBits().get(this.getSystemIndex());
		boolean interested = true; // possibly interested, let's try to prove it wrong.
		
		BitSet componentBits = e.getComponentBits();

		// Check if the entity possesses ALL of the components defined in the aspect.
		if(!allSet.isEmpty()) {
			for (int i = allSet.nextSetBit(0); i >= 0; i = allSet.nextSetBit(i+1)) {
				if(!componentBits.get(i)) {
					interested = false;
					break;
				}
			}
		}
		
		// Check if the entity possesses ANY of the exclusion components, if it does then the system is not interested.
		if(!exclusionSet.isEmpty() && interested) {
			interested = !exclusionSet.intersects(componentBits);
		}
		
		// Check if the entity possesses ANY of the components in the oneSet. If so, the system is interested.
		if(!oneSet.isEmpty()) {
			interested = oneSet.intersects(componentBits);
		}

		if (interested && !contains) {
			insertToSystem(e);
		} else if (!interested && contains) {
			removeFromSystem(e);
		}
	}

	private void removeFromSystem(Entity e) {
		actives.remove(e);
		e.getSystemBits().clear(this.getSystemIndex());
		removed(e);
	}

	private void insertToSystem(Entity e) {
		actives.add(e);
		e.getSystemBits().set(this.getSystemIndex());
		inserted(e);
	}
	
	
	@Override
	public final void added(Entity e) {
		check(e);
	}
	
	@Override
	public final void changed(Entity e) {
		check(e);
	}
	
	@Override
	public final void deleted(Entity e) {
		if(e.getSystemBits().get(this.getSystemIndex())) {
			removeFromSystem(e);
		}
	}
	
	@Override
	public final void disabled(Entity e) {
		if(e.getSystemBits().get(this.getSystemIndex())) {
			removeFromSystem(e);
		}
	}
	
	@Override
	public final void enabled(Entity e) {
		check(e);
	}
	
	public ImmutableBag<Entity> getActives() {
		return actives;
	}
}
