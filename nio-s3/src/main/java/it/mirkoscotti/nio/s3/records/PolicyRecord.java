package it.mirkoscotti.nio.s3.records;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.json.bind.annotation.JsonbProperty;

import io.github.mirkoscotti.nio.s3.enums.BucketAction;
import io.github.mirkoscotti.nio.s3.enums.BucketEffect;

/**
 * @author mirko.scotti
 * @version Oct 25, 2024
 */
public record PolicyRecord(@JsonbProperty("Version") String version,
						   @JsonbProperty("Statement") List<StatementRecord> statements)
{

	public boolean isReadOnly()
	{
		// If some action contains a wild-card, all actions matching with the
		// pattern must be treated as they were explicitly specified
		var map = Optional.ofNullable(statements)
						  .orElseGet(List::of)
						  .stream()
						  .flatMap(Stream::of)
						  .collect(Collectors.groupingBy(item -> BucketEffect.of(item.effect())))
						  .entrySet()
						  .stream()
						  .collect(Collectors.toMap(Entry::getKey,
													item -> matchingActions(item.getValue())));
		// If at least one allow is present and none of them match write actions, then the method
		// must return true because the default deny
		var allowedActions = map.getOrDefault(Optional.of(BucketEffect.ALLOW), Set.of());
		var deniedActions = map.getOrDefault(Optional.of(BucketEffect.DENY), Set.of());
		// If write actions are present in the allow section and also in the deny, the item in the
		// allow section must be ignored and treated as it was not present
		var set = new HashSet<>(allowedActions);
		set.removeAll(deniedActions);
		// If no statements are present, the bucket owner can perform all actions, thus the method
		// must return false. If case of at least one allowed action, each of the set must not match
		// write actions
		return !map.isEmpty() && BucketAction.writeActions().noneMatch(set::contains);
	}

	private Set<BucketAction> matchingActions(List<StatementRecord> actions)
	{
		return actions.stream()
					  .flatMap(item -> item.actions().stream())
					  .flatMap(BucketAction::matchingActions)
					  .collect(Collectors.toSet());
	}
}
