package gameoflife;

import java.util.List;
import kotlinx.coroutines.channels.Channel;

public record CellOptions(
        int row,
        int col,
        boolean alive,
        Channel<Boolean> tickChannel,
        Channel<Boolean> resultChannel,
        List<Channel<Boolean>> inChannels,
        List<Channel<Boolean>> outChannels) {}
