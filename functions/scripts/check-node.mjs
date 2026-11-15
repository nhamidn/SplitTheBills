const [major, minor] = process.versions.node.split(".").map(Number);

if (major < 22 || (major === 22 && minor < 12)) {
  console.error(
    `Node ${process.versions.node} is too old for the pinned Firebase backend dependencies.\n` +
      "Use Node 22.12 or newer (22.13.1 is pinned in ../.nvmrc), then start again.",
  );
  process.exit(1);
}
