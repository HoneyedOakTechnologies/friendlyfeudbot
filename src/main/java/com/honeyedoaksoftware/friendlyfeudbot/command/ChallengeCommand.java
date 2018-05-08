package com.honeyedoaksoftware.friendlyfeudbot.command;

import com.honeyedoaksoftware.friendlyfeudbot.Model.Challenge;
import com.honeyedoaksoftware.friendlyfeudbot.repository.ChallengeRepository;
import com.honeyedoaksoftware.friendlyfeudbot.util.BotUtils;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IIDLinkedObject;
import sx.blah.discord.handle.obj.IUser;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j
@Component
public class ChallengeCommand implements Command {

	public static final String COMMAND_TEXT = "challenge";

	private ChallengeRepository challengeRepository;

	@Autowired
	public ChallengeCommand(ChallengeRepository challengeRepository) {
		this.challengeRepository = challengeRepository;
	}

	@Override
	public void runCommand(MessageReceivedEvent event, String[] args) {
		IChannel channel = event.getChannel();
		long guildId = event.getGuild().getLongID();

		log.debug("Challenge declared in guild " + guildId + "! received the following arguments: " + Arrays.toString(args));

		if (event.getAuthor().isBot()) {
			BotUtils.sendMessage(channel, "Darn Tootin! How did a bot issue a challenge? Mis me with that gay shit!!");
		}

		long challengerUserId = event.getAuthor().getLongID();

		boolean defenderError = false;
		if (args.length < 1) {
			defenderError = true;
		}
		Optional<Long> defenderUserId = BotUtils.getIdFromText(args[0]);

		if (!defenderUserId.isPresent()) {
			defenderError = true;
		}

		if (defenderError) {
			BotUtils.sendMessage(channel, BotUtils.userLongIdToMention(challengerUserId) + " You need to declare a defender @ mention someone else");
			return;
		}

		if (challengerUserId == defenderUserId.get()) {
			BotUtils.sendMessage(channel, BotUtils.userLongIdToMention(challengerUserId) + " Challenging yourself are you? 1v1 someone else!");
			return;
		}

		Optional<Long> refereeUserId = Optional.empty();
		if (args.length > 1) {
			refereeUserId = BotUtils.getIdFromText(args[1]);
		}

		if (refereeUserId.isPresent()) {
			if (Objects.equals(challengerUserId, refereeUserId.get()) || Objects.equals(defenderUserId.get(), refereeUserId.get())) {
				BotUtils.sendMessage(channel, BotUtils.userLongIdToMention(challengerUserId) + " The referee should be independent, do not pick either party as the referee. (a referee is not mandatory)");
				return;
			}
		}

		List<Long> mentionedBots = event.getMessage().getMentions().stream().filter(IUser::isBot).map(IIDLinkedObject::getLongID).collect(Collectors.toList());

		if (mentionedBots.contains(defenderUserId.get())) {
			BotUtils.sendMessage(channel, BotUtils.userLongIdToMention(challengerUserId) + " Do you honestly expect a bot to accept a challenge? please pick a human adversary!!");
			return;
		}

		if (refereeUserId.isPresent() && mentionedBots.contains(refereeUserId.get())) {
			BotUtils.sendMessage(channel, BotUtils.userLongIdToMention(challengerUserId) + " How do you expect a bot to referee a challenge? please pick a human witness!!");
			return;
		}

		String challengeText = Arrays.stream(args).filter(arg -> !arg.matches("<@\\d*>")).collect(Collectors.joining(" "));

		if (StringUtils.isBlank(challengeText)) {
			BotUtils.sendMessage(channel, BotUtils.userLongIdToMention(challengerUserId) + " just shouting names won't do, add a challenge description");
			return;
		}

		Challenge challenge = new Challenge(null, guildId, challengerUserId, defenderUserId.get(), refereeUserId.orElse(null), challengeText, null, null);

		challenge = challengeRepository.save(challenge);

		log.trace("challenge saved: " + challenge.toString());

		StringBuilder message = new StringBuilder("Pistols at dawn! A challenge has been posted. ")
				.append(BotUtils.userLongIdToMention(challengerUserId))
				.append(" has Challenged ")
				.append(BotUtils.userLongIdToMention(defenderUserId.get()))
				.append(" To a duel.\n")
				.append("The duel will consist of \"").append(challengeText).append("\"");

		refereeUserId.ifPresent(aLong -> message.append("\n").append(BotUtils.userLongIdToMention(aLong)).append(" has been chosen as witness and arbiter"));

		BotUtils.sendMessage(channel, message.toString());
	}
}
