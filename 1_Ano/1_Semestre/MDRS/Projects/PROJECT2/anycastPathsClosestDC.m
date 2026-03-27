function sP_any = anycastPathsClosestDC(L, Ta, anycastNodes)
    nAny = size(Ta,1);
    sP_any = cell(1, nAny);

    for f = 1:nAny
        s = Ta(f,1);

        bestCost = inf;
        bestPath = [];

        for a = 1:length(anycastNodes)
            dc = anycastNodes(a);

            try
                [p, c] = kShortestPath(L, s, dc, 1);
            catch
                p = {};
                c = [];
            end

            % Se não houver caminho, ignora este DC
            if isempty(c) || isempty(p) || isempty(p{1})
                continue;
            end

            if c(1) < bestCost
                bestCost = c(1);
                bestPath = p{1};
            end
        end

        if isempty(bestPath)
            error("Anycast: não existe caminho do nó %d para nenhum DC [%d %d]. Verifica L/Links.", ...
                  s, anycastNodes(1), anycastNodes(2));
        end

        sP_any{f} = {bestPath};
    end
end
