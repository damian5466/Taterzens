package org.samo_lego.taterzens.common.commands.edit;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.samo_lego.taterzens.common.commands.NpcCommand;
import org.samo_lego.taterzens.common.Taterzens;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.synchronization.SuggestionProviders.SUMMONABLE_ENTITIES;
import static org.samo_lego.taterzens.common.Taterzens.config;
import static org.samo_lego.taterzens.common.compatibility.ModDiscovery.DISGUISELIB_LOADED;
import static org.samo_lego.taterzens.common.util.TextUtil.successText;
import static org.samo_lego.taterzens.common.util.TextUtil.translate;

import org.samo_lego.taterzens.common.npc.NPCData;
import static org.apache.logging.log4j.LogManager.getLogger;


public class TypeCommand {

    public final NPCData npcData = new NPCData();

    public static void registerNode(LiteralCommandNode<CommandSourceStack> editNode, CommandBuildContext commandBuildContext) {
        LiteralCommandNode<CommandSourceStack> typeNode = literal("type")
                .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.entity_type", config.perms.npcCommandPermissionLevel))
                .then(argument("entity type", ResourceArgument.resource(commandBuildContext, Registries.ENTITY_TYPE))
                        .suggests(SUMMONABLE_ENTITIES)
                        .executes(TypeCommand::changeType)
                        .then(argument("nbt", CompoundTagArgument.compoundTag())
                                .executes(TypeCommand::changeType)
                        )
                )
                .then(literal("minecraft:player")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.entity_type", config.perms.npcCommandPermissionLevel))
                        .executes(TypeCommand::resetType)
                )
                .then(literal("player")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.entity_type", config.perms.npcCommandPermissionLevel))
                        .executes(TypeCommand::resetType)
                )
                .then(literal("reset")
                        .requires(src -> Taterzens.getInstance().getPlatform().checkPermission(src, "taterzens.npc.edit.entity_type", config.perms.npcCommandPermissionLevel))
                        .executes(TypeCommand::resetType)
                )
                .build();

        editNode.addChild(typeNode);
    }

    private static int changeType(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        if (!DISGUISELIB_LOADED) {
            source.sendFailure(translate("advert.disguiselib.required")
                    .withStyle(ChatFormatting.RED)
                    .withStyle(style -> style
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translate("advert.tooltip.install", "DisguiseLib")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/mod/disguiselib"))
                    )
            );
            return -1;
        }

        ResourceLocation disguise = ResourceArgument.getSummonableEntityType(context, "entity type").key().location();
        return NpcCommand.selectedTaterzenExecutor(source.getEntityOrException(), taterzen -> {
            CompoundTag nbt;
            try {
                nbt = CompoundTagArgument.getCompoundTag(context, "nbt").copy();
            } catch(IllegalArgumentException ignored) {
                nbt = new CompoundTag();
            }
            nbt.putString("id", disguise.toString());

            EntityType.loadEntityRecursive(nbt, source.getLevel(), (entityx) -> {
                // We can do some manipulation based on the detail returned
                // taterzen.modEntity() is a short all-caps descriptor of the current TYPE.
                var removeSub = "entity.minecraft.";
				
		    	var interim = entityx.getType().getDescriptionId();
		    	
		    	
		    	String stripped = interim.replace(removeSub, "").toUpperCase();
		    	
		    	getLogger("Taterzens").info("[Taterzens]: Setting the order to change the type - {}", stripped);

		    	taterzen.modEntity(stripped);

                Taterzens.getInstance().getPlatform().disguiseAs(taterzen, entityx);
                source.sendSuccess(() ->
                                translate(
                                        "taterzens.command.entity_type.set",
                                        Component.translatable(entityx.getType().getDescriptionId()).withStyle(ChatFormatting.YELLOW)
                                ).withStyle(ChatFormatting.GREEN),
                        false
                );
                return entityx;
            });
        });
    }

    private static int resetType(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        if(!DISGUISELIB_LOADED) {
            source.sendFailure(translate("advert.disguiselib.required")
                    .withStyle(ChatFormatting.RED)
                    .withStyle(style -> style
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translate("advert.tooltip.install", "DisguiseLib")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/mod/disguiselib"))
                    )
            );
            return -1;
        }
        return NpcCommand.selectedTaterzenExecutor(source.getEntityOrException(), taterzen -> {
            
        	// The Reset is to PLAYER
        	taterzen.modEntity("PLAYER");
            
            Taterzens.getInstance().getPlatform().clearDisguise(taterzen);
            source.sendSuccess(() ->
                            successText("taterzens.command.entity_type.reset", taterzen.getName().getString()),
                    false
            );
        });
    }

}
