function linkLoadsA = computeLinkLoadsAnycast(Ta, anycastNodes, L, nNodes)
% Computes link loads contributed by anycast flows.
% Anycast rule (Project): each anycast flow is routed on the shortest path
% to the closest anycast node (ties -> first node in anycastNodes).
%
% Ta: [source, upstream, downstream]
% anycastNodes: [a1 a2]
% L: link length matrix (Km), inf for no-link
% nNodes: number of nodes

linkLoadsA = zeros(nNodes);

for f = 1:size(Ta,1)
    s = Ta(f,1);
    up = Ta(f,2);
    down = Ta(f,3);

    % --- choose closest anycast node by shortest-path distance ---
    a1 = anycastNodes(1);
    a2 = anycastNodes(2);

    d1 = dijkstraDistance(L, s, a1);
    d2 = dijkstraDistance(L, s, a2);

    if d1 <= d2
        dest = a1;
    else
        dest = a2;
    end

    % --- path is shortest path s -> dest ---
    if s == dest
        path = s; % trivial, adds no load
    else
        path = dijkstraPath(L, s, dest);
        if isempty(path)
            % No path exists: treat as infeasible upstream (caller decides)
            % Here we just skip; better is to signal infeasible outside.
            continue;
        end
    end

    % add upstream load along s->dest
    for i = 1:length(path)-1
        a = path(i);
        b = path(i+1);
        linkLoadsA(a,b) = linkLoadsA(a,b) + up;
    end

    % add downstream load along dest->s (reverse path)
    for i = length(path):-1:2
        a = path(i);
        b = path(i-1);
        linkLoadsA(a,b) = linkLoadsA(a,b) + down;
    end
end
end