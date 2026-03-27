function [sP_any, nSP_any, Ta_routing] = computeAnycastShortestToClosest(nNodes, anycastNodes, L, Ta)
% Anycast: para cada source em Ta, escolhe o shortest path para o anycast node MAIS PRÓXIMO.
% Devolve:
% - sP_any: paths por fluxo anycast
% - nSP_any: nº de paths por fluxo (será 1 para os fluxos anycast considerados)
% - Ta_routing: matriz [src dst up down] (dst = anycast escolhido)

    % Primeiro, obter o melhor path (k=1) para cada fonte que tenha fluxo anycast
    [sP_nodes, nSP_nodes] = bestAnycastPaths(nNodes, anycastNodes, L, Ta);

    nFlows_any = size(Ta,1);
    sP_any = cell(1, nFlows_any);
    nSP_any = ones(1, nFlows_any); % each anycast flow uses exactly 1 (shortest) path

    Ta_routing = [Ta(:,1) zeros(nFlows_any,1) Ta(:,2:3)]; % [src dst up down]

    for i = 1:nFlows_any
        src = Ta(i,1);

        % No teu bestAnycastPaths original, o output foi “compactado”.
        % Para ficar robusto, vamos recalcular diretamente para cada fluxo:
        bestCost = inf;
        bestPath = [];

        for a = 1:length(anycastNodes)
            [p, c] = kShortestPath(L, src, anycastNodes(a), 1);
            if ~isempty(c) && c(1) < bestCost
                bestCost = c(1);
                bestPath = p;
            end
        end

        sP_any{i} = bestPath;          % cell {i} contém {1} path
        Ta_routing(i,2) = bestPath{1}(end); % destino = anycast escolhido
    end
end