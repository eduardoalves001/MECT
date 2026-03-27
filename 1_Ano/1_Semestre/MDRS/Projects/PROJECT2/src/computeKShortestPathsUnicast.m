function [sP_uni, nSP_uni] = computeKShortestPathsUnicast(Tu, L, k)
% Determina os k-shortest paths (por comprimento) para cada fluxo unicast.
    nFlows = size(Tu, 1);
    sP_uni = cell(1, nFlows);
    nSP_uni = zeros(1, nFlows);

    for f = 1:nFlows
        [paths, costs] = kShortestPath(L, Tu(f,1), Tu(f,2), k);
        sP_uni{f} = paths;
        nSP_uni(f) = length(costs);
    end
end
