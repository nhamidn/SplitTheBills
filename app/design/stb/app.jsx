// App shell: state, routing, transitions

function StbApp({ initial = { tab: 'home', stack: [] }, persist = false, settleInit = null }) {
  const load = () => {
    if (!persist) return initial;
    try { return JSON.parse(localStorage.getItem('stb-nav')) || initial; } catch { return initial; }
  };
  const [nav, setNav] = React.useState(load);
  const [expenses, setExpenses] = React.useState(STB_EXPENSES_SEED);
  const [settlements, setSettlements] = React.useState(STB_SETTLEMENTS_SEED);
  const [settle, setSettle] = React.useState(settleInit);
  const [snack, setSnack] = React.useState(null);

  React.useEffect(() => { if (persist) localStorage.setItem('stb-nav', JSON.stringify(nav)); }, [nav]);
  React.useEffect(() => { if (!snack) return; const t = setTimeout(() => setSnack(null), 2600); return () => clearTimeout(t); }, [snack]);

  const toast = (text) => setSnack({ text, id: Date.now() });
  const app = {
    groups: STB_GROUPS_SEED, expenses, settlements,
    tab: (t) => setNav({ tab: t, stack: [] }),
    push: (r) => setNav((n) => ({ ...n, stack: [...n.stack, r] })),
    back: () => setNav((n) => ({ ...n, stack: n.stack.slice(0, -1) })),
    toast,
    openSettle: (d) => setSettle(d),
    settle: (d) => {
      setSettlements((s) => [...s, { id: 's' + Date.now(), group: d.group, from: d.from, to: d.to, amount: r2(d.amount), date: STB_TODAY, method: d.method, status: 'completed' }]);
      toast(`Settled ${stbFmt(d.amount)} with ${STB_PEOPLE[d.from === 'me' ? d.to : d.from].short}`);
    },
    addExpense: (e) => {
      setExpenses((x) => [...x, e]);
      setNav((n) => ({ ...n, stack: n.stack.slice(0, -1) }));
      toast(`“${e.title}” added · ${stbFmt(e.amount)}`);
    },
  };

  const route = nav.stack[nav.stack.length - 1];
  let screen;
  if (!route) {
    screen = nav.tab === 'home' ? <HomeScreen app={app} /> : nav.tab === 'groups' ? <GroupsScreen app={app} /> : nav.tab === 'activity' ? <ActivityScreen app={app} /> : <ProfileScreen app={app} />;
  } else if (route.name === 'group') screen = <GroupScreen app={app} route={route} />;
  else if (route.name === 'add') screen = <AddExpenseScreen app={app} route={route} />;
  else if (route.name === 'balances') screen = <BalancesScreen app={app} />;

  return (
    <div className="stb" style={{ position: 'relative', height: '100%', overflow: 'hidden', background: T.bg, color: T.ink, fontFamily: T.font }}>
      <div key={nav.tab + '/' + nav.stack.map((r) => r.name + (r.id || '')).join('/')} style={{ position: 'absolute', inset: 0 }}>
        {screen}
      </div>
      {!route && <BottomNav tab={nav.tab} onTab={app.tab} />}
      <Snackbar msg={snack} />
      <Sheet open={!!settle} onClose={() => setSettle(null)}>
        {settle && <SettleSheet key={JSON.stringify(settle)} app={app} d={settle} onClose={() => setSettle(null)} />}
      </Sheet>
    </div>
  );
}

function StbPhone({ children, width = 412, height = 892 }) {
  return (
    <AndroidDevice width={width} height={height}>
      <div style={{ position: 'relative', height: '100%' }}>{children}</div>
    </AndroidDevice>
  );
}

Object.assign(window, { StbApp, StbPhone });
