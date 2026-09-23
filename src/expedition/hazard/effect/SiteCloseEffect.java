/*
 * SiteCloseEffect
 * Effect for code CLOSE_SITE — closes the current drill site and
 * force-ends the session.
 */
package expedition.hazard.effect;

public record SiteCloseEffect() implements Effect {
    
    @Override
    public void apply(EffectContext ctx) {
        // checks if the site is currently being used and if it isnt then it doesn nothing
        if (ctx.session() == null) {
        }
        else 
        {
            // if it is currently being used it forces the drilling to end and closes the site
            ctx.session().closeSiteAndEnd(); 
        }
    }
}
