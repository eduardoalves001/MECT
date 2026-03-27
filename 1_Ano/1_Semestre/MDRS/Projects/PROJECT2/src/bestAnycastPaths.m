function [sP, nSP] = bestAnycastPaths(nNodes, anycastNodes, L, Ta)
% Devolve para cada nó que aparece em Ta(:,1) o shortest path para o anycast mais próximo.
    sP = cell(1, nNodes);
    nSP = zeros(1, nNodes);

    for n = 1:nNodes
        if ismember(n, anycastNodes)
            nSP(n) = -1;
            continue;
        end

        if ~ismember(n, Ta(:, 1))
            nSP(n) = -1;
            continue;
        end

        best = inf;
        for a = 1:length(anycastNodes)
            [shortestPath, totalCost] = kShortestPath(L, n, anycastNodes(a), 1);

            if ~isempty(totalCost) && totalCost(1) < best
                sP{n} = shortestPath;
                nSP(n) = length(totalCost);
                best = totalCost(1);
            end
        end
    end

    nSP = nSP(nSP~=-1);
    sP = sP(~cellfun(@isempty, sP));
end